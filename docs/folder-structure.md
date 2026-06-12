claimlens/

├── backend/
│
├── frontend/
│
├── ocr-service/
│
├── analysis-service/
│
├── infra/
│
├── docs/
│
├── docker-compose.yml
│
├── .gitignore
│
└── README.md


com.niyotechnologies.claimlens



backend/

└── src/main/java/com/niyotechnologies/claimlens

    ├── config/
    │
    ├── security/
    │
    ├── tenancy/
    │
    ├── common/
    │
    ├── shared/
    │
    ├── auth/
    │
    ├── organization/
    │
    ├── user/
    │
    ├── policy/
    │
    ├── product/
    │
    ├── claim/
    │
    ├── document/
    │
    ├── assignment/
    │
    ├── investigation/
    │
    ├── processing/
    │
    ├── fraud/
    │
    ├── notification/
    │
    ├── analytics/
    │
    ├── audit/
    │
    ├── events/
    │
    ├── outbox/
    │
    ├── scheduler/
    │
    ├── integration/
    │
    └── policyintelligence/




    claim/

├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── validator/
├── event/
├── exception/
└── specification/



claim/

├── controller/
│   └── ClaimController
│
├── service/
│   ├── ClaimService
│   └── ClaimServiceImpl
│
├── repository/
│   ├── ClaimRepository
│   ├── ClaimHistoryRepository
│   └── ClaimCommentRepository
│
├── entity/
│   ├── Claim
│   ├── ClaimHistory
│   └── ClaimComment
│
├── dto/
├── mapper/
├── validator/
├── event/
├── exception/
└── specification/



backend/

└── src/main/resources

    ├── application.yml

    ├── db/
    │   └── migration/
    │
    ├── logback-spring.xml
    │
    └── banner.txt







    infra/

├── docker/
│
├── monitoring/
│   ├── prometheus/
│   └── grafana/
│
├── nginx/
│
└── scripts/


And after adding the new domains, the final module list became:


auth
organization
accesscontrol
user
policy
product
claim
document
assignment
investigation
processing
fraud
notification
analytics
audit
policyintelligence