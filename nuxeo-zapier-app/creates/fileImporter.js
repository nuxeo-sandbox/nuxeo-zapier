const FormData = require('form-data');
const request = require('request');

const fileImporter = (z, bundle) => {
  const automationBody = {
    'params': {},
    'context': {
      'currentDocument': bundle.inputData.parent,
    },
    'input': {},
  };
  const formData = new FormData();
  formData.append('automationBody', JSON.stringify(automationBody));
  let body = request(bundle.inputData.file);
  body.name = bundle.inputData.filename;
  body.filename = bundle.inputData.filename;
  formData.append('input', body);
  const responsePromise = z.request({
    url: `${bundle.authData.url}/nuxeo/site/automation/FileManager.Import`,
    method: 'POST',
    body: formData,
    headers: {
      'properties': '*',
      'repository': bundle.inputData.repository,
      'enrichers.document': 'documentURL',
    },
  });
  return responsePromise.then((response) => {
    const document = JSON.parse(response.content);
    document.id = document.uid;
    return document;
  });
};

module.exports = {
  key: 'fileImporter',
  noun: 'Import file',

  display: {
    label: 'Import File',
    description: 'Import a file by creating a new document with a file attached',
    important: true,
  },

  operation: {
    inputFields: [
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/api/v1/search/lang/NXQL/execute?query=SELECT * FROM Document WHERE ecm:mixinType = \'Folderish\' AND ecm:mixinType <> \'SystemDocument\'`,
          params: {},
        };
        return z.request(request).then((response) => {
          const folders = JSON.parse(response.content).entries;
          let result = {};
          result.key = 'parent';
          result.label = 'File Location';
          result.helpText = 'The file parent path or id';
          result.choices = {};
          result.required = true;
          result.altersDynamicFields = true;
          folders.forEach((folder) => {
            result.choices[folder.uid] = folder.path;
          });
          return result;
        });
      }, {
        key: 'file',
        required: true,
        label: 'File',
        type: 'file',
        helpText: 'The file to attach',
      }, {
        key: 'filename',
        required: true,
        label: 'File name',
      }, {
        key: 'repository',
        required: true,
        label: 'Repository name',
        default: 'default',
      },
    ],
    perform: fileImporter,
  },
};
