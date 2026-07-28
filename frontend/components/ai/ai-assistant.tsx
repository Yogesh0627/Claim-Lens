"use client";

import { useEffect, useRef, useState } from "react";
import { Bot, ChevronDown, ChevronRight, Loader2, RotateCcw, Send, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAsync } from "@/hooks/useAsync";
import { isApiError } from "@/lib/http";
import type { AskCoverageResponse } from "@/lib/types";

/** One selectable thing to ask about — a policy (customer) or a product (staff). */
export interface AssistantContext {
  id: number;
  label: string;
  sublabel?: string;
}

interface Message {
  role: "user" | "assistant";
  text: string;
  citations?: { snippet: string }[];
}

/**
 * A floating, role-agnostic coverage assistant. It only knows how to pick a context and ask a
 * question — the caller supplies what the contexts are (their policies / the tenant's products) and
 * how to ask. Answers come from the RAG over policy wording and always carry citations.
 */
export function AiAssistant({
  title = "Coverage assistant",
  contextLabel,
  loadContexts,
  ask,
  suggestions = [],
  emptyContexts,
}: {
  title?: string;
  contextLabel: string;
  loadContexts: () => Promise<AssistantContext[]>;
  ask: (contextId: number, question: string) => Promise<AskCoverageResponse>;
  suggestions?: string[];
  emptyContexts: string;
}) {
  const [open, setOpen] = useState(false);
  const [contextId, setContextId] = useState<string>("");
  const [input, setInput] = useState("");
  const [asking, setAsking] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const scrollRef = useRef<HTMLDivElement>(null);

  const contexts = useAsync(() => loadContexts(), [open], { enabled: open });
  const list = contexts.data ?? [];

  // Default-select the first context once the list arrives.
  useEffect(() => {
    if (contexts.data && contexts.data.length > 0 && !contextId) {
      setContextId(String(contexts.data[0].id));
    }
  }, [contexts.data, contextId]);

  // Keep the newest message in view.
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, asking]);

  const submit = async (question: string) => {
    const q = question.trim();
    if (!q || !contextId || asking) return;
    setInput("");
    setMessages((m) => [...m, { role: "user", text: q }]);
    setAsking(true);
    try {
      const res = await ask(Number(contextId), q);
      setMessages((m) => [
        ...m,
        { role: "assistant", text: res.answer, citations: res.citations },
      ]);
    } catch (e) {
      const msg = isApiError(e) ? e.message : "Something went wrong — please try again.";
      setMessages((m) => [...m, { role: "assistant", text: msg }]);
      toast.error(msg);
    } finally {
      setAsking(false);
    }
  };

  const newChat = () => {
    setMessages([]);
    setInput("");
  };

  return (
    <>
      <Button
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-40 h-12 gap-2 rounded-full px-5 shadow-lg"
      >
        <Sparkles className="h-4 w-4" /> Ask AI
      </Button>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="right" className="flex w-full flex-col gap-0 p-0 sm:max-w-md">
          <SheetHeader className="border-b">
            <SheetTitle className="flex items-center gap-2">
              <Bot className="h-5 w-5" /> {title}
            </SheetTitle>
            <SheetDescription>
              Ask about coverage and policy wording — answers cite the exact terms.
            </SheetDescription>
          </SheetHeader>

          <div className="border-b p-4">
            {contexts.loading ? (
              <p className="text-muted-foreground text-sm">Loading…</p>
            ) : list.length === 0 ? (
              <p className="text-muted-foreground text-sm">{emptyContexts}</p>
            ) : (
              <div className="flex items-end gap-2">
                <div className="grid min-w-0 flex-1 gap-1.5">
                  <label className="text-muted-foreground text-xs font-medium">{contextLabel}</label>
                  <Select
                    value={contextId}
                    onValueChange={(v) => {
                      setContextId(v);
                      setMessages([]);
                    }}
                  >
                    <SelectTrigger className="cursor-pointer">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {list.map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.label}
                          {c.sublabel ? ` · ${c.sublabel}` : ""}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className="shrink-0"
                  onClick={newChat}
                  disabled={messages.length === 0 && !input}
                  title="New chat"
                >
                  <RotateCcw className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>

          <div ref={scrollRef} className="min-w-0 flex-1 space-y-4 overflow-y-auto p-4">
            {messages.length === 0 ? (
              <div className="space-y-3">
                <p className="text-muted-foreground text-sm">Try asking:</p>
                <div className="flex flex-wrap gap-2">
                  {suggestions.map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => submit(s)}
                      disabled={!contextId || asking}
                      className="border-border hover:bg-muted cursor-pointer rounded-full border px-3 py-1 text-xs transition-colors disabled:opacity-50"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              messages.map((m, i) => <MessageBubble key={i} m={m} />)
            )}
            {asking ? (
              <div className="text-muted-foreground flex items-center gap-2 text-sm">
                <Loader2 className="h-4 w-4 animate-spin" /> Thinking…
              </div>
            ) : null}
          </div>

          <div className="border-t p-4">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                submit(input);
              }}
              className="flex items-center gap-2"
            >
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder={contextId ? "Ask a question…" : "Select above to begin"}
                disabled={!contextId || asking}
              />
              <Button
                type="submit"
                size="icon"
                className="shrink-0"
                disabled={!contextId || asking || !input.trim()}
              >
                <Send className="h-4 w-4" />
              </Button>
            </form>
            <p className="text-muted-foreground mt-2 text-[10px]">
              AI answers from the policy wording and can be imperfect — check the cited terms.
            </p>
          </div>
        </SheetContent>
      </Sheet>
    </>
  );
}

function MessageBubble({ m }: { m: Message }) {
  const [showSources, setShowSources] = useState(false);

  if (m.role === "user") {
    return (
      <div className="flex justify-end">
        <div className="bg-primary text-primary-foreground max-w-[85%] overflow-hidden rounded-lg rounded-br-sm px-3 py-2 text-sm break-words">
          {m.text}
        </div>
      </div>
    );
  }

  const citations = m.citations ?? [];
  return (
    <div className="flex justify-start">
      <div className="bg-muted max-w-[90%] min-w-0 space-y-2 overflow-hidden rounded-lg rounded-bl-sm px-3 py-2 text-sm">
        <p className="break-words whitespace-pre-wrap">{m.text}</p>
        {citations.length > 0 ? (
          <div className="border-t pt-2">
            <button
              type="button"
              onClick={() => setShowSources((s) => !s)}
              className="text-muted-foreground hover:text-foreground flex cursor-pointer items-center gap-1 text-[11px] font-medium"
            >
              {showSources ? (
                <ChevronDown className="h-3 w-3" />
              ) : (
                <ChevronRight className="h-3 w-3" />
              )}
              {showSources ? "Hide" : "Show"} sources ({citations.length})
            </button>
            {showSources ? (
              <div className="mt-2 space-y-1.5">
                {citations.map((c, i) => (
                  <p
                    key={i}
                    className="text-muted-foreground line-clamp-3 border-l-2 pl-2 text-xs break-words italic"
                  >
                    {c.snippet}
                  </p>
                ))}
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}
