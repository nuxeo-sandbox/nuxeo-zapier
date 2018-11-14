const projectTrigger = require('./triggers/project');
const deliverableSetTrigger = require('./triggers/deliverableSet');
const taskCreate = require('./creates/task');
const oauth = require('./auth/oauth');
const AuditHook = require('./triggers/AuditHook');

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
    [projectTrigger.key]: projectTrigger,
    [deliverableSetTrigger.key]: deliverableSetTrigger,
    [AuditHook.key]: AuditHook,
  },

  searches: {},

  creates: {
    [taskCreate.key]: taskCreate,
  },
};

module.exports = App;
