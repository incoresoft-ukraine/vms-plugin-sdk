# VMS Plugin SDK

Documentation and templates for building **third-party plugins for Incoresoft VMS**, release line
**25.1**. Everything here was checked against the VMS host and the sample plugin that the templates
form; where a statement is a recommendation rather than observed behaviour, the text says so.

> **Стислий вступ українською.** Плагін VMS складається з двох частин: Java-jar для бекенду
> (розширення `VmsExtension`, Guice, Javalin 5, jOOQ/Liquibase) та Vue 3 бандла для фронтенду
> (Webpack Module Federation), який упаковується всередину того ж jar під `public/`. Бекенд
> реєструє REST/WS-ендпоінти, права, типи правил і тривог, сповіщення, обʼєкти для карт і планів;
> фронтенд декларує у `component.json`, які компоненти вставляти у слоти інтерфейсу VMS (вкладки,
> пошук, тривоги, сповіщення, правила, карти, комірки розкладки). Документація написана
> англійською, бо цільова аудиторія — зовнішні розробники.

## Contents

| # | Document | What you get |
|---|---|---|
| 00 | [Overview](docs/00-overview.md) ([UK](docs/00-overview.uk.md)) | Two pages for decision makers: capabilities, requirements, process, rules |
| 01 | [Architecture](docs/01-architecture.md) | What a plugin is, plugin types, identifiers, lifecycle, how the web client loads a plugin |
| 02 | [Getting started](docs/02-getting-started.md) | Prerequisites, the sample plugin in thirty minutes, the development loop, troubleshooting |
| 03 | [Backend](docs/03-backend.md) | `pom.xml` and manifest, `VmsExtension`, Guice, database, REST, WebSocket, host services, permissions, rules and alarms, retention, cameras, maps |
| 04 | [Frontend](docs/04-frontend.md) | Module Federation setup, `component.json`, `settings.json`, layouts and routes, API calls, locales, every UI slot with its props and events |
| 05 | [Packaging and release](docs/05-packaging-and-release.md) | Jar layout, versioning, CI outline, the host HTTP API for plugin management |

## Templates

| Path | Purpose |
|---|---|
| `templates/backend-plugin-template/` | The sample plugin's backend for VMS 25.1: categories (with a VMS camera) managed in Admin Center, items created by operators, permissions, a rule type with alarms and notifications, a live WebSocket feed, retention, a paged and filterable API that also serves the Search page. Ships a build of the frontend template in `src/main/resources/public/`, so `mvn package` gives a complete plugin. Published as its own repository: [vms-plugin-backend-template](https://github.com/incoresoft-ukraine/vms-plugin-backend-template) |
| `templates/frontend-plugin-minimal/` | The sample plugin's frontend: an Admin Center section, a client tab with pages and archive video, header "+" item, admin sidebar item, icon, rule-wizard blocks, alarm, notification, Search and layout-cell slots, locales `en`/`uk`/`es`. Installs from the public npm registry (`@incoresoft/incoresoft-ui`, `-icons`, `-toasts` `^25.1.0`). Published as its own repository: [vms-plugin-frontend-template](https://github.com/incoresoft-ukraine/vms-plugin-frontend-template) |
| `tools/build-pdf.mjs` | Builds the PDFs (Developer Guide, Overview EN/UK) from the markdown: `node tools/build-pdf.mjs` (needs a `node_modules` with `micromark` and `micromark-extension-gfm`, see `MICROMARK_NODE_MODULES` in the script) |

## Reading order

New to VMS: 01 → 02, then 03 and 04 while coding, 05 when shipping.

## Deliverables

| File | Audience | Contents |
|---|---|---|
| `VMS-Plugin-SDK-Overview.pdf` (EN), `VMS-Plugin-SDK-Overview.uk.pdf` (UK) | technical decision makers | `docs/00-overview` |
| `VMS-Plugin-SDK-Developer-Guide.pdf` and this repository | partner developers | README, `docs/00–05`, templates |

The documentation mentions Incoresoft's own plugins (POS terminals, access control) as the origin
of a pattern. Their sources are not part of this SDK; example plugin sources can be shared with
partners on request.

## License

Documentation: CC BY 4.0. Templates and tools: MIT. The Java libraries (`middleware`,
`device-driver-sdk`) are published on Maven Central under the Apache License 2.0. The npm packages
(`@incoresoft/incoresoft-ui`, `-icons`, `-toasts`) are distributed under the Incoresoft VMS Plugin
SDK License shipped with them. See `LICENSE`.

Support: support@incoresoft.com
