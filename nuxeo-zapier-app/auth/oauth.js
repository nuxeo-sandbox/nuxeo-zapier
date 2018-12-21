/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
const getAccessToken = (z, bundle) => {
  const promise = z.request(`${bundle.inputData.url}/nuxeo/oauth2/access-token`, {
    method: 'POST',
    body: {
      code: bundle.inputData.code,
      client_id: 'nuxeo-zapier',
      client_secret: bundle.inputData.secret,
      grant_type: 'authorization_code',
      redirect_uri: 'https://zapier.com/dashboard/auth/oauth/return/App7494CLIAPI/',
    },
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
    },
  });
  return promise.then((response) => {
    if (response.status !== 200) {
      throw new Error('Unable to fetch access token: ' + response.content);
    }
    const result = JSON.parse(response.content);
    return {
      access_token: result.access_token,
      refresh_token: result.refresh_token,
    };
  });
};

const testAuth = (z, bundle) => {
  const promise = z.request({
    method: 'GET',
    url: `${bundle.authData.url}/nuxeo/json/cmis`,
  });
  return promise.then((response) => {
    if (response.status === 401) {
      throw new Error('The access token you supplied is not valid');
    }
    return z.JSON.parse(response.content);
  });
};

const refreshAccessToken = (z, bundle) => {
  const promise = z.request(`${bundle.authData.url}/nuxeo/oauth2/token`, {
    method: 'POST',
    params: {
      refresh_token: bundle.authData.refresh_token,
      client_id: 'nuxeo-zapier',
      client_secret: bundle.authData.secret,
      grant_type: 'refresh_token'
    },
    headers: {
      'content-type': 'application/x-www-form-urlencoded'
    }
  });
  return promise.then((response) => {
    if (response.status !== 200) {
      throw new Error('Unable to fetch access token: ' + response.content);
    }

    const result = JSON.parse(response.content);
    return {
      access_token: result.access_token
    };
  });
};

module.exports = {
  type: 'oauth2',
  oauth2Config: {
    // Step 1
    authorizeUrl: {
      url: `{{bundle.inputData.url}}/nuxeo/oauth2/authorize`,
      params: {
        client_id: 'nuxeo-zapier',
        redirect_uri: '{{bundle.inputData.redirect_uri}}',
        response_type: 'code',
      },
    },
    // Step 2
    getAccessToken: getAccessToken,
    refreshAccessToken: refreshAccessToken,
    // Invoke `refreshAccessToken` on a 401 response
    autoRefresh: true,
    // scope: 'read,write',
  },
  fields:
    [
      {
        key: 'url',
        required: true,
        type: 'string',
      },
      {
        key: 'secret',
        required: true,
        type: 'string',
      },
    ],
  test: testAuth,
  connectionLabel: '- Connected via OAuth',
};
