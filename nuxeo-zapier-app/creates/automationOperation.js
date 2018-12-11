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

const runOperation = (z, bundle) => {
  return fetchParameters(z, bundle.inputData.operation, bundle).then((entries) => {
    // Building parameters listing to send
    let parameters = {};
    entries.filter((entry) => bundle.inputData[entry.name]).forEach((entry) => {
      parameters[entry.name] = bundle.inputData[entry.name];
    });
    return fetchInputs(z, bundle.inputData.operation, bundle).then((entries) => {
      // Getting input to send
      let input = '';
      const inputId = entries.find((entry) => bundle.inputData[entry]);
      if (inputId) {
        input = bundle.inputData[inputId];
      }

      // Building Automation request
      const automationRequest = {
        method: 'POST',
        url: `${bundle.authData.url}/nuxeo/api/v1/automation/${bundle.inputData.operation}`,
        headers: {
          'Content-Type': 'application/json',
          'properties': '*',
          'repository': bundle.inputData.repository,
          'enrichers.document': 'documentURL',
        },
      };

      const automationBody = {
        context: {},
        input: input,
        params: parameters,
      };

      // If input is a blob, send multipart
      if (inputId === 'blob') {
        // Convert body/request to multipart
        automationBody.input = {};
        automationRequest.headers['Content-Type'] = 'multipart/form-data';
        // Multipart
        const formData = new FormData();
        formData.append('automationBody', JSON.stringify(automationBody));
        const body = request(bundle.inputData.blob);
        // body.name = 'blob';
        // body.filename = 'blob';
        formData.append('input', body);
        automationRequest.body = formData;
      } else {
        // Else send json payload
        automationRequest.body = JSON.stringify(automationBody);
      }

      const responsePromise = z.request(automationRequest);
      return responsePromise.then((response) => {
        // if 204 or a blob as a result: no response content to return
        let result = {};
        result.id = 'result';
        result.result = 'operation succeeded';
        let contentType = JSON.parse(JSON.stringify(response.headers))['content-type']; // must be cloned for unknown reason
        if (response.status !== 204 && contentType.includes('application/json')) {
          result = JSON.parse(response.content);
        }
        return result;
      });
    });
  });
};

const fetchParameters = (z, operationId, bundle) => {
  return z.request(getRequest(bundle, operationId)).then((response) => {
    return JSON.parse(response.content).params;
  });
};

const fetchInputs = (z, operationId, bundle) => {
  return z.request(getRequest(bundle, operationId)).then((response) => {
    const signature = JSON.parse(response.content).signature;
    let inputs = [];
    for (let i = 0; i < signature.length; i += 2) {
      inputs.push(signature[i]);
    }
    return inputs.filter((input) => {
      return !['void', 'bloblist'].includes(input);
    });
  });
};

const fetchDescription = (z, operationId, bundle) => {
  return z.request(getRequest(bundle, operationId)).then((response) => {
    return JSON.parse(response.content).description;
  });
};

const getRequest = (bundle, operationId = '') => {
  return {
    headers: {
      'Content-Type': 'application/json',
    },
    url: `${bundle.authData.url}/nuxeo/site/automation/${operationId}`,
    params: {},
  }
}

module.exports = {
  key: 'automation',
  noun: 'Automation',

  display: {
    label: 'Run Automation Operation',
    description: 'Run Automation any operation with given input(s) and parameter(s)',
    important: true,
  },

  operation: {
    inputFields: [
      // Operation ID
      function (z, bundle) {
        return z.request(getRequest(bundle)).then((response) => {
          let operations = JSON.parse(response.content).operations;
          // Removing UI operations and Chains
          operations = operations.filter((operation) => {
            return !['User Interface', 'Chain'].includes(operation.category) && 'Seam' !== operation.requires;
          });
          let result = {};
          result.key = 'operation';
          result.helpText = 'Select the operation to run';
          result.label = 'Operation ID';
          result.choices = {};
          result.required = true;
          result.altersDynamicFields = true;
          operations.forEach((operation) => {
            result.choices[operation.id] = operation.id;
          });
          return result;
        });
      },
      // Operation Description (display only)
      function (z, bundle) {
        const operationId = bundle.inputData.operation;
        if (operationId) {
          return fetchDescription(z, operationId, bundle).then((description) => {
            if (description && description !== '') {
              let result = {};
              result.key = 'description';
              result.helpText = description;
              result.type = 'copy';
              return [result];
            }
            return [];
          });
        }
        return [];
      },
      // Operation Inputs
      function (z, bundle) {
        const operationId = bundle.inputData.operation;
        if (operationId) {
          return fetchInputs(z, operationId, bundle).then((inputs) => {
            return inputs.map((input) => {
              let result = {};
              switch (input) {
                case 'blob':
                  result.type = 'file';
                  break;
                case 'documents':
                  result.list = true;
                  break;
                default:
                  break;
              }
              result.key = input;
              result.label = `Input: ${input}`;
              return result;
            });
          });
        }
        return [];
      },
      // Operation Parameters
      function (z, bundle) {
        const operationId = bundle.inputData.operation;
        if (operationId) {
          return fetchParameters(z, operationId, bundle).then((parameters) => {
            let results = [];
            parameters.forEach((param) => {
              let result = {};
              result.key = param.name;
              result.label = param.name;
              result.type = param.type;
              result.helpText = param.description;
              result.required = param.required;
              results.push(result);
            });
            return results;
          });
        }
        return [];
      },
    ],
    perform: runOperation,
  },
};
