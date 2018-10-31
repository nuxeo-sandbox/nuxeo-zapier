/* Just the Skeleton ... */

const createTask = (z, bundle) => {
  const responsePromise = z.request({
    method: 'POST',
    url: 'http://nightly.nuxeo.com/nuxeo/api/v1/automation/NBM.CreateTask',
    body: JSON.stringify({
      title: bundle.inputData.title,
    }),
  });
  return responsePromise
    .then((response) => JSON.parse(response.content));
};

module.exports = {
  key: 'task',
  noun: 'Task',

  display: {
    label: 'Create Task',
    description: 'Creates a task.',
  },

  operation: {
    inputFields: [],
    perform: createTask,
  },
};
