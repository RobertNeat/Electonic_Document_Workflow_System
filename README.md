<p align="center">
  <img src="apps/document_web/public/document.png" width="150" alt="EDWS logo">
</p>

<h1 align="center">Electronic Document Workflow System</h1>

<p align="center">
  <a href="https://robertneat.github.io/Electonic_Document_Workflow_System/">
    <img src="https://img.shields.io/badge/Documentation-4d8c42?style=for-the-badge" alt="Documentation">
  </a>
  <br>
  <a href="https://github.com/RobertNeat/Electonic_Document_Workflow_System/blob/main/docs/README_pl.md">
    <img src="https://img.shields.io/badge/Polska_wersja_dokumentu-4d8c42?style=for-the-badge" alt="Polska wersja">
  </a>
</p>

Enterprise-grade electronic document and case management system, modeled on EZD (Elektroniczne Zarządzanie Dokumentacją) systems used in Polish public administration. Built as a microservices architecture - a central case/document API, a dedicated DMS, a JasperReports rendering engine, an electronic signature service with its own demonstration PKI, a delivery simulator, Keycloak, and RabbitMQ - to mirror how a real administrative back office is split across specialized systems rather than one monolith.

## Document workflow standards used

- **JRWA** - Jednolity Rzeczowy Wykaz Akt (uniform records classification scheme) drives case reference numbers and archival category (A/B) assignment.
- **DMS (document management)** - versioned storage of document templates and generated documents, with a clear separation between draft (unsigned) and final (signed) versions of a document.
- **XAdES / PAdES (ETSI)** - electronic signing of PDF (PAdES) and XML (XAdES) documents using the EU DSS library, backed by a demonstration PKI (CA hierarchy, OCSP/CRL responder); XAdES is implemented in the signing service but not yet wired up end to end.
- **OIDC / OAuth2** - Keycloak as the identity provider; `document_api` validates JWTs as an OAuth2 resource server and maps realm roles to Spring Security authorities for role-based access control.
- **EZD architecture** - case register, case history (metryka sprawy), incoming/outgoing correspondence register, and audit logging follow the conventions of Polish EZD-class systems.
- **CAMT.053 (ISO 20022)** - bank statement import with real parsing of the standard structure and automatic matching of payments to receivables.
- **EDI/XML** - import of case messages (XML) and cost documents/invoices (UBL/PEPPOL) from external parties, with structural validation; currently import-only and without a live PEPPOL network connection.

## What EDWS does

- **Case and workflow management** - register cases under configurable workflow definitions (states, transitions, required roles/documents per type of matter), track case history, and enforce role- and document-based gating on state transitions.

![Case management workflow](/docs/media/1.%20przeprowadzenie%20sprawy/przeprowadzenie_sprawy.gif)

- **Document and template management (DMS)** - upload and version JasperReports templates, generate documents from a template and case data, and browse the version history of a document, distinguishing draft (unsigned) from final (signed) versions.

![Template upload by admin](/docs/media/2.%20dokumenty%20i%20podpis/1.%20wgranie%20szablonu%20jasper%20przez%20administratora.png)

- **Electronic signatures** - sign generated PDF documents with a user certificate, verify signature status (valid/invalid/expired/revoked) via OCSP/CRL, and inspect signature details including signer, timestamp, and certificate fingerprint.

![Electronic signature](/docs/media/2.%20dokumenty%20i%20podpis/złożenie_podpisu.gif)

- **Correspondence and dispatch** - send signed letters to citizens or organizations through e-Delivery, postal hybrid mail, or e-mail, and track delivery status (sent/delivered/received/returned) through an asynchronous status feed.

![Correspondence](/docs/media/3.%20wysyłka%20korespondencji/wysyłka_korespondencji.gif)

- **Payment reconciliation** - register receivables tied to a case, import bank statements to automatically match incoming payments, manually reconcile unmatched payments, and generate overdue payment reminders.

![Payment](/docs/media/4.%20płatności/1.%20przegląd%20rejestru%20należności.png)

- **Citizen/entity card** - search citizens and organizations and view a unified profile covering their basic data, cases, documents, correspondence, and outstanding balances in one place.

![Citizen card](/docs/media/5.%20karta%20obywatelapodmiotu/karta_obywatela.gif)

- **EDI/XML exchange** - import case messages (XML) and cost documents/invoices (UBL/PEPPOL) from external parties, with structural validation and mapping into the domain model.
- **Bulk actions** - run bulk state changes, bulk letter dispatch, or bulk document generation across multiple cases asynchronously, with progress tracking.
- **User dashboard** - a landing page summarising open cases, upcoming/overdue statutory deadlines, unsettled receivables, and pending deliveries for the logged-in user.
- **Administration** - manage user profiles and roles, reference dictionaries (JRWA, document types, dispatch providers, fee rates), and review the audit log of operations on cases, documents, and payments.
