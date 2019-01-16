/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const hydrators = require('../hydrators');
const subscribeHook = (z, bundle) => {
  const data = {
    url: bundle.targetUrl,
    resolver: 'documentCreated',
    requiredFields: getRequiredFields(bundle.inputData.docTypes),
  };
  const options = {
    url: `${bundle.authData.url}/nuxeo/site/hook`,
    method: 'POST',
    body: JSON.stringify(data),
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const getRequiredFields = (docTypes) => {
  return {
    sourceType: docTypes.join(),
  }
};

const unsubscribeHook = (z, bundle) => {
  const hookId = bundle.subscribeData.id;
  const options = {
    url: `${bundle.authData.url}/nuxeo/site/hook/${hookId}`,
    method: 'DELETE',
  };
  return z.request(options).then((response) => JSON.parse(response.content));
};

const getNotifications = (z, bundle) => {
  const notification = bundle.cleanedRequest;
  if ('binary' in notification[0]) {
    notification[0].binary = z.dehydrate(hydrators.downloadFile, {
      url: notification[0].binary,
    });
  }
  return notification;
};

const triggerNotificationWebHook = (z, bundle) => {
  const request = {
    headers: {
      'Content-Type': 'application/json',
    },
    url: `${bundle.authData.url}/nuxeo/site/hook/example?${getQueryListParams('schemas', bundle.inputData.schemas)}`,
  };
  return z.request(request).then((response) => {
    return z.JSON.parse(response.content);
  });
};

const getQueryListParams = (id, list) => {
  let params = '';
  list.forEach((entry) => {
    params += `${id}=${entry}&`;
  });
  return params;
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
      // Schemas to fetch from the sample
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/api/v1/config/schemas`,
          params: {},
        };
        return z.request(request).then((response) => {
          const schemas = JSON.parse(response.content);
          let entries = {};
          entries.key = 'schemas';
          entries.label = 'Schemas';
          entries.helpText = 'Choose schema(s) to map for the next template';
          entries.choices = {};
          entries.list = true;
          entries.default = 'dublincore';
          schemas.forEach((schema) => {
            entries.choices[schema.name] = schema.name;
          });
          return entries;
        });
      },
    ],

    performSubscribe: subscribeHook,
    performUnsubscribe: unsubscribeHook,

    perform: getNotifications,
    performList: triggerNotificationWebHook,
  },
};
