const subscribeHook = (z, bundle) => {
  const data = {
    url: bundle.targetUrl,
    event: bundle.inputData.events,
  };
  const options = {
    url: 'http://zapier.apps.prod.nuxeo.io/nuxeo/site/hook',
    method: 'POST',
    body: JSON.stringify(data),
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const unsubscribeHook = (z, bundle) => {
  const hookId = bundle.subscribeData.id;
  const options = {
    url: `http://zapier.apps.prod.nuxeo.io/nuxeo/site/hook/${hookId}`,
    method: 'DELETE',
  };
  return z.request(options)
    .then((response) => JSON.parse(response.content));
};

const getAuditEvent = (z, bundle) => {
  // bundle.cleanedRequest will include the parsed JSON object (if it's not a
  // test poll) and also a .querystring property with the URL's query string.
  const auditEvent = {
    id: bundle.cleanedRequest.uid,
    docUUID: bundle.cleanedRequest.docUUID,
    docLifeCycle: bundle.cleanedRequest.docLifeCycle,
    docPath: bundle.cleanedRequest.docPath,
    principalName: bundle.cleanedRequest.principalName,
  };

  return [auditEvent];
};

const triggerAuditHook = (z, bundle) => {
  const request = {
    url: 'http://zapier.apps.prod.nuxeo.io/nuxeo/site/hook/auditexample',
    params: {},
  };
  return z.request(request).then((response) => {
    return z.JSON.parse(response.content);
  });
};

module.exports = {
  key: 'auditHook',
  noun: 'AuditHook',

  display: {
    label: 'Get Audit Event',
    description: 'Receive Audit Event',
  },

  operation: {
    type: 'hook',

    inputFields: [
      {
        key: 'events',
        required: true,
        list: true,
        choices: {documentCreated: 'Creation event', documentModified: 'Modification event'},
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
