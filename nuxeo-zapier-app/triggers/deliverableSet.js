const triggerDeliverableSet = (z, bundle) => {
  const request = {
    url: 'http://nightly.nuxeo.com/nuxeo/api/v1/search/pp/list_project_deliverable_sets/execute',
    params: {},
  };
  z.console.log('Input:' + JSON.stringify(bundle.inputData));
  request.params.queryParams = bundle.inputData.path;
  return z.request(request).then((response) => {
    let results = z.JSON.parse(response.content).entries;
    return results.map((deliverableSet) => {
      deliverableSet.id = deliverableSet.uid;
      delete deliverableSet.uid;
      return deliverableSet;
    });
  });
};

module.exports = {
  key: 'deliverableset',
  noun: 'DeliverableSet',

  display: {
    label: 'Get Deliverable Set',
    description: 'Poll new deliverable sets',
  },

  operation: {
    inputFields: [
      {
        key: 'path',
        type: 'string',
        helpText: 'A given project path',
        dynamic: 'project.path',
        altersDynamicFields: true,
      },
    ],
    perform: triggerDeliverableSet,
    outputFields: [
      {key: 'id', label: 'ID'},
      {key: 'created', label: 'Created At'},
      {key: 'title', label: 'Title'},
      {key: 'path', label: 'Path'},
    ],
  },
};
