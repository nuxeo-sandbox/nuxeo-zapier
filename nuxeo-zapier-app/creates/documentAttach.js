/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const FormData = require('form-data');
const request = require('request');

const documentAttach = (z, bundle) => {
  const automationBody = {
    'params': {
      'document': bundle.inputData.document,
      'xpath': bundle.inputData.xpath,
    },
    'context': {},
    'input': {},
  };
  const formData = new FormData();
  formData.append('automationBody', JSON.stringify(automationBody));
  let body = request(bundle.inputData.file);
  body.name = bundle.inputData.filename;
  body.filename = bundle.inputData.filename;
  formData.append('input', body);
  const responsePromise = z.request({
    url: `${bundle.authData.url}/nuxeo/site/automation/Blob.AttachOnDocument`,
    method: 'POST',
    body: formData,
    headers: {
      'properties': '*',
      'repository': bundle.inputData.repository,
      'enrichers.document': 'documentURL',
    },
  });
  return responsePromise.then(() => {
    const result = {};
    result.id = 'result';
    result.result = 'blob attached';
    return result;
  });
};

module.exports = {
  key: 'documentAttach',
  noun: 'Attach a file',

  display: {
    label: 'Attach File',
    description: 'Attach a file to a given document',
  },

  operation: {
    inputFields: [
      {
        key: 'document',
        required: true,
        label: 'Document',
        helpText: 'The given document to update',
      }, {
        key: 'file',
        required: true,
        label: 'File',
        type: 'file',
        helpText: 'The file to attach to the given document',
      }, {
        key: 'filename',
        required: true,
        label: 'File name',
      }, {
        key: 'xpath',
        label: 'Xpath property',
        default: 'file:content',
        helpText: 'The xpath property of the given document',
      }, {
        key: 'repository',
        required: true,
        label: 'Repository name',
        default: 'default',
      },
    ],
    perform: documentAttach,
  },
};
