/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const documentCreation = (z, bundle) => {
  const responsePromise = z.request({
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'properties': '*',
      'repository': bundle.inputData.repository,
      'enrichers.document': 'documentURL',
    },
    url: `${bundle.authData.url}/nuxeo/api/v1/automation/Document.Create`,
    body: JSON.stringify({
      context: {},
      input: bundle.inputData.parent,
      params: {
        type: bundle.inputData.type,
        name: bundle.inputData.name,
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
  key: 'documentCreation',
  noun: 'Document Creation',

  display: {
    label: 'Create Document',
    description: 'Create a new document with a given location and properties',
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
          result.label = 'Document Location';
          result.helpText = 'The document parent path or id';
          result.required = true;
          result.choices = {};
          result.altersDynamicFields = true;
          folders.forEach((folder) => {
            result.choices[folder.uid] = folder.path;
          });
          return result;
        });
      }, {
        key: 'name',
        required: true,
        label: 'Document name',
        helpText: 'The document path name',
      },
      function (z, bundle) {
        const request = {
          url: `${bundle.authData.url}/nuxeo/api/v1/config/types`,
          params: {},
        };
        return z.request(request).then((response) => {
          const types = JSON.parse(response.content).doctypes;
          let result = {};
          result.key = 'type';
          result.label = 'Document type';
          result.helpText = 'Select the document type of this new document';
          result.choices = {};
          result.default = 'File';
          result.altersDynamicFields = true;
          Object.keys(types).forEach((key) => {
            result.choices[key] = key;
          });
          return result;
        });
      },
      {
        key: 'properties',
        label: 'Properties',
        type: 'text',
      }, {
        key: 'repository',
        required: true,
        label: 'Repository name',
        default: 'default',
      },
    ],
    perform: documentCreation,
  },
};
