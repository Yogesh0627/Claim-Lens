"use client";

import { useState } from "react";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useMutation } from "@/hooks/useMutation";
import { coverageService } from "@/services/coverageService";
import type { AskCoverageResponse } from "@/lib/types";

const SUGGESTIONS = [
  "Is windshield glass damage covered?",
  "What is required to claim for theft?",
  "Are there any exclusions I should know about?",
];

export function CoverageTab({ claimId }: { claimId: number }) {
  const [question, setQuestion] = useState("");
  const [result, setResult] = useState<AskCoverageResponse | null>(null);

  const ask = useMutation((q: string) => coverageService.ask({ claimId, question: q }), {
    onSuccess: (r) => setResult(r),
  });

  const submit = (q: string) => {
    const text = q.trim();
    if (!text) return;
    setQuestion(text);
    ask.run(text);
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-sm font-medium">
            <Sparkles className="h-4 w-4" /> Ask about this policy&apos;s coverage
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Textarea
            rows={2}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. Is windshield damage covered under this policy?"
          />
          <div className="flex flex-wrap items-center gap-2">
            <Button onClick={() => submit(question)} disabled={ask.loading || !question.trim()}>
              {ask.loading ? "Thinking…" : "Ask"}
            </Button>
            {SUGGESTIONS.map((s) => (
              <Button
                key={s}
                type="button"
                variant="outline"
                size="sm"
                className="text-muted-foreground h-auto rounded-full px-2.5 py-1 text-xs font-normal"
                onClick={() => submit(s)}
              >
                {s}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {result ? (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Answer</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm whitespace-pre-wrap">{result.answer}</p>
            <p className="text-muted-foreground text-xs">Model: {result.model}</p>

            {result.citations.length > 0 ? (
              <div className="space-y-2">
                <p className="text-xs font-semibold">Grounded in policy excerpts</p>
                {result.citations.map((c, i) => (
                  <div key={c.policyChunkId} className="rounded-md border p-2 text-xs">
                    <div className="text-muted-foreground mb-1 flex justify-between">
                      <span>
                        [{i + 1}] chunk #{c.chunkIndex}
                      </span>
                      {c.score != null ? <span>score {c.score.toFixed(3)}</span> : null}
                    </div>
                    <p className="whitespace-pre-wrap">{c.snippet}</p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground text-xs">
                No policy wording has been ingested for this product version yet.
              </p>
            )}
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
