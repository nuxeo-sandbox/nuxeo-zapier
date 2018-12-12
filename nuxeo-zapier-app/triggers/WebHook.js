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
    resolver: bundle.inputData.resolver,
    requiredFields: getRequiredFields(z, bundle.inputData),
  };
  const options = {
    url: `${bundle.authData.url}/nuxeo/site/hook`,
    method: 'POST',
    body: JSON.stringify(data),
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const getRequiredFields = (inputData) => {
  delete inputData.resolver;
  return inputData;
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

const fetchResolver = (z, operationId, bundle) => {
  return z.request(getRequest(bundle, operationId)).then((response) => {
    return JSON.parse(response.content).requiredFields;
  });
};

const getRequest = (bundle, notificationId) => {
  return {
    headers: {
      'Content-Type': 'application/json',
    },
    url: `${bundle.authData.url}/nuxeo/api/v1/notification/resolver/${notificationId}`,
    params: {},
  }
}

module.exports = {
  key: 'webHook',
  noun: 'Notifications WebHook',

  display: {
    label: 'Get Nuxeo Notifications',
    description: 'Get Nuxeo Notifications',
  },

  operation: {
    type: 'hook',

    inputFields: [
      // Resolver
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/api/v1/notification/resolver`,
          params: {},
        };
        return z.request(request).then((response) => {
          const resolvers = JSON.parse(response.content).entries;
          let result = {};
          result.key = 'resolver';
          result.helpText = 'Choose resolvers';
          result.choices = {};
          result.required = true;
          result.altersDynamicFields = true;
          resolvers.forEach((resolver) => {
            result.choices[resolver.id] = resolver.id;
          });
          return result;
        });
      },
      // Resolver required fields
      function (z, bundle) {
        const resolverId = bundle.inputData.resolver;
        if (resolverId) {
          return fetchResolver(z, resolverId, bundle).then((requiredFields) => {
            let results = [];
            requiredFields.forEach((requiredField) => {
              let result = {};
              result.key = requiredField;
              result.label = requiredField;
              result.required = true;
              results.push(result);
            });
            return results;
          });
        }
        return [];
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