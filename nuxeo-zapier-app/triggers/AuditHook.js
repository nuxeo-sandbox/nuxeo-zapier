/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const subscribeHook = (z, bundle) => {
  const data = {
    url: bundle.targetUrl,
    events: bundle.inputData.events,
    docTypes: bundle.inputData.docTypes,
  };
  const options = {
    url: `${bundle.authData.url}/nuxeo/site/hook`,
    method: 'POST',
    body: JSON.stringify(data),
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const unsubscribeHook = (z, bundle) => {
  const hookId = bundle.subscribeData.id;
  const options = {
    url: `${bundle.authData.url}/nuxeo/site/hook/${hookId}`,
    method: 'DELETE',
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const getAuditEvent = (z, bundle) => {
  return bundle.cleanedRequest;
};

const triggerAuditHook = (z, bundle) => {
  const request = {
    url: `${bundle.authData.url}/nuxeo/site/hook/auditexample`,
    params: {},
  };
  return z.request(request).then((response) => {
    return z.JSON.parse(response.content);
  });
};

module.exports = {
  key: 'auditHook',
  noun: 'Events Hook',

  display: {
    label: 'Get Nuxeo Events',
    description: 'Intercept Nuxeo events for given document types',
  },

  operation: {
    type: 'hook',

    inputFields: [
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/site/hook/events`,
          params: {},
        };
        return z.request(request).then((response) => {
          const events = JSON.parse(response.content);
          let entries = {};
          entries.key = 'events';
          entries.helpText = 'Choose the events to filter on';
          entries.choices = {};
          entries.list = true;
          entries.default = 'documentCreated';
          events.forEach((value) => {
            entries.choices[value] = value;
          });
          return entries;
        });
      },
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/api/v1/config/types`,
          params: {},
        };
        return z.request(request).then((response) => {
          const types = JSON.parse(response.content).doctypes;
          let entries = {};
          entries.key = 'docTypes';
          entries.label = 'Document Types';
          entries.helpText = 'Choose the document types to filter on';
          entries.choices = {};
          entries.list = true;
          entries.default = 'File';
          Object.keys(types).forEach((key) => {
            entries.choices[key] = key;
          });
          return entries;
        });
      },
    ],

    performSubscribe: subscribeHook,
    performUnsubscribe: unsubscribeHook,

    perform: getAuditEvent,
    performList: triggerAuditHook,
    outputFields: [
      {key: 'id', label: 'ID'},
      {key: 'docUUID', label: 'Document UUID'},
      {key: 'docLifeCycle', label: 'Document State'},
      {key: 'docPath', label: 'Document Path'},
      {key: 'principalName', label: 'Who'},
    ],
  },
};
