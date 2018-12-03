const startWorkflow = (z, bundle) => {
  const responsePromise = z.request({
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'properties': '*',
      'repository': bundle.inputData.repository,
      'enrichers.document': 'documentURL',
    },
    url: `${bundle.authData.url}/nuxeo/api/v1/automation/Context.StartWorkflow`,
    body: JSON.stringify({
      context: {},
      input: bundle.inputData.documents,
      params: {
        id: bundle.inputData.name,
      },
    }),
  });
  return responsePromise.then((response) => {
    const blob = JSON.parse(response.content);
    blob.id = blob.uid;
    return blob;
  });
};

module.exports = {
  key: 'startWorkflow',
  noun: 'Startworkflow',

  display: {
    label: 'Start Workflow on Document(s)',
    description: 'Start a given workflow on given document(s)',
  },

  operation: {
    inputFields: [
      {
        key: 'documents',
        label: 'Document(s)',
        list: true,
        helpText: 'The given document(s) to start the workflow on',
      }, {
        key: 'name',
        label: 'Workflow name',
        default: 'SerialDocumentReview',
        required: true,
      }, {
        key: 'repository',
        required: true,
        label: 'Repository name',
        default: 'default',
      },
    ],
    perform: startWorkflow,
  },
};
