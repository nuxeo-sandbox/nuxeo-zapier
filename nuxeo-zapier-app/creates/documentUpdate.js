/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const documentUpdate = (z, bundle) => {
  const responsePromise = z.request({
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'properties': '*',
      'repository': bundle.inputData.repository,
      'enrichers.document': 'documentURL',
    },
    url: `${bundle.authData.url}/nuxeo/api/v1/automation/Document.Update`,
    body: JSON.stringify({
      context: {},
      input: bundle.inputData.document,
      params: {
        properties: bundle.inputData.properties,
      },
    }),
  });
  return responsePromise.then((response) => {
    const document = JSON.parse(response.content);
    document.id = document.uid;
    return document;
  });
};

module.exports = {
  key: 'documentUpdate',
  noun: 'Document Update',

  display: {
    label: 'Update Document',
    description: 'Update a given document',
    important: true,
  },

  operation: {
    inputFields: [
      {
        key: 'document',
        required: true,
        label: 'Document ID or Path',
      }, {
        key: 'repository',
        required: true,
        label: 'Repository name',
        default: 'default',
      },
      {
        key: 'properties',
        required: true,
        label: 'Properties',
        type: 'text',
      },
    ],
    perform: documentUpdate,
  },
};
