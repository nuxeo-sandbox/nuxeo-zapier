# Zapier App for Nuxeo Platform

This plugin contains:

- The Nuxeo platform addon to setup the webhook.
- The Zapier plugin itself with triggers and actions for Nuxeo Platform.

## Zapier Nuxeo Webhook

Here is the future description of the webhook.

## Zapier App

Here is the future description of the Zapier app features.

### Specifications

https://jira.nuxeo.com/browse/NXP-26022

### The structure

```
├── README.md
├── auth
│   ├── basic.js
│   └── oauth.js
├── creates - "the actions"
│   └── task.js - not implemented
├── index.js
├── package.json
├── test
│   ├── authentication.js
│   └── project.js
└── triggers
    ├── AuditHook.js - "the audit trigger via hooks"
    ├── deliverableSet.js - "the deliverable set trigger via polling"
    ├── event.js - "the static dropdown for choosing events"
    └── project.js - "the project dropdown for choosing a given project"
```

## Contribute

Here is the future way of contributing to the project.

## Usage

Here is the future way of using the plugin.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) 

(C) Copyright Nuxeo Corp. (http://nuxeo.com/)

All images, icons, fonts, and videos contained in this folder are copyrighted by Nuxeo, all rights reserved.

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.