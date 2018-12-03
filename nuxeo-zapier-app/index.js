/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const oauth = require('./auth/oauth');
const AutomationOperation = require('./creates/automationOperation');
const AuditHook = require('./triggers/AuditHook');
const DocumentCreation = require('./creates/documentCreation');
const DocumentUpdate = require('./creates/documentUpdate');
const DocumentAttach = require('./creates/documentAttach');
const StartWorkflow = require('./creates/startWorkflow');
const FileManager = require('./creates/fileImporter');

const handleHTTPError = (response, z) => {
  if (response.status >= 400) {
    throw new Error(`Unexpected status code ${response.status}`);
  }
  return response;
};

// To include the Authorization header on all outbound requests
const includeBearerToken = (request, z, bundle) => {
  if (bundle.authData.access_token) {
    request.headers.Authorization = `Bearer ${bundle.authData.access_token}`;
  }
  return request;
};

const App = {
  version: require('./package.json').version,
  platformVersion: require('zapier-platform-core').version,
  authentication: oauth,

  beforeRequest: [
    includeBearerToken,
  ],

  afterResponse: [
    handleHTTPError,
  ],

  resources: {},

  triggers: {
    [AuditHook.key]: AuditHook,
  },

  searches: {},

  creates: {
    [AutomationOperation.key]: AutomationOperation,
    [DocumentCreation.key]: DocumentCreation,
    [DocumentUpdate.key]: DocumentUpdate,
    [DocumentAttach.key]: DocumentAttach,
    [StartWorkflow.key]: StartWorkflow,
    [FileManager.key]: FileManager,
  },
};

module.exports = App;
