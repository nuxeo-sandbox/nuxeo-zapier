const hydrators = require('../hydrators');
const FormData = require('form-data');
const request = require('request');

const spikeBlobCreate = (z, bundle) => {
  request(bundle.inputData.file, function (error, response, body) {
    // console.log('error:', error); // Print the error if one occurred
    // console.log('statusCode:', response && response.statusCode); // Print the response status code if a response was received
    z.console.log('1:' + body.replace('hello', ''));
    const automationBody = {
      'params': {},
      'context': {
        'currentDocument': '/default-domain/workspaces/workspace'
      },
      'input': {},
    };
    const formData = new FormData();
    formData.append('automationBody', JSON.stringify(automationBody));
    formData.append('input', request(body.replace('hello', '')));
    z.console.log('2:' + JSON.stringify(formData));
    return z.request({
      url: 'http://zapier.apps.prod.nuxeo.io/nuxeo/site/automation/FileManager.Import',
      method: 'POST',
      body: formData,
    })
      .then((response) => {
        // The json document definition returned by nuxeo but apparently we should return here the hydrated file (don't know why)
        const documentJSON = response.json;
        const document = JSON.parse(documentJSON);
        document.id = document.uid;
        return document;
        // let file = {};
        // file.file = z.dehydrateFile(hydrators.downloadFile, {
        //   fileId: file.id,
        // });
        // return file;
      });
  });
};

module.exports = {
  key: 'spikeBlobCreate',
  noun: 'File',
  display: {
    label: 'Upload File',
    description: 'Uploads a file.'
  },
  operation: {
    inputFields: [
      {
        key: 'name',
        required: false,
        type: 'string',
        label: 'Name',
        helpText: 'If not defined, the Filename will be copied here.'
      },
      {key: 'filename', required: true, type: 'string', label: 'Filename'},
      {key: 'file', required: true, type: 'file', label: 'File'},
    ],
    perform: spikeBlobCreate,
    sample: {
      id: 1,
      name: 'Example PDF',
      file: 'SAMPLE FILE',
      filename: 'example.pdf',
    },
    outputFields: [
      {key: 'id', type: 'integer', label: 'ID'},
      {key: 'name', type: 'string', label: 'Name'},
      {key: 'filename', type: 'string', label: 'Filename'},
      {key: 'file', type: 'file', label: 'File'},
    ],
  }
};
