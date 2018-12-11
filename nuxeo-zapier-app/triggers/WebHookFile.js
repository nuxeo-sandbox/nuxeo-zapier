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
    resolver: 'documentCreated',
    requiredFields: bundle.inputData.docTypes,
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

const getNotifications = (z, bundle) => {
  return bundle.cleanedRequest;
};

const triggerNotificationWebHook = (z, bundle) => {
  const request = {
    url: `${bundle.authData.url}/nuxeo/site/hook/example`,
    params: {},
  };
  return z.request(request).then((response) => {
    return z.JSON.parse(response.content);
  });
};

module.exports = {
  key: 'webHookFileCreated',
  noun: 'Notifications File Creation',

  display: {
    label: 'Get Nuxeo Document Creations',
    description: 'Getting all notifications on document creations in Nuxeo for given type(s)',
  },

  operation: {
    type: 'hook',

    inputFields: [
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

    perform: getNotifications,
    performList: triggerNotificationWebHook,
    outputFields: [
      {key: 'id', label: 'ID'},
      {key: 'docUUID', label: 'Document UUID'},
      {key: 'docLifeCycle', label: 'Document State'},
      {key: 'docPath', label: 'Document Path'},
      {key: 'principalName', label: 'Who'},
    ],
  },
};
