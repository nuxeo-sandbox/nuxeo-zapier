const triggerProject = (z, bundle) => {
  const request = {
    url: 'http://nightly.nuxeo.com/nuxeo/api/v1/search/pp/list_projects/execute',
    params: {},
  };

  return z.request(request).then((response) => {
    const projects = JSON.parse(response.content).entries;
    return projects.map((project) => {
      project.id = project.uid;
      delete project.uid;
      return project;
    });
  });
};

module.exports = {
  key: 'project',
  noun: 'Project',

  display: {
    label: 'List of Projects',
    description: 'The only purpose of this trigger is to populate the dropdown list of projects in the UI',
    hidden: true,
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
    perform: triggerProject,
  },
};
