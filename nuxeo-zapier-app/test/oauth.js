require('should');

const zapier = require('zapier-platform-core');

const App = require('../index');
const appTester = zapier.createAppTester(App);

describe('oauth2 app', () => {
  it('generates an authorize URL', () => {
    const bundle = {
      inputData: {
        redirect_uri: getRedirectURI,
        state: 'stateless'
      },
      environment: {
        CLIENT_ID: getClientId,
        CLIENT_SECRET: getSecret(),
      }
    };

    bundle.inputData.url = getURL();

    return appTester(App.authentication.oauth2Config.authorizeUrl, bundle)
      .then((authorizeUrl) => {
        authorizeUrl.should.eql('http://zapier.apps.prod.nuxeo.io/nuxeo/oauth2/authorize?client_id=nuxeo-zapier&state=stateless&' +
          'redirect_uri=https%3A%2F%2Fzapier.com%2Fdashboard%2Fauth%2Foauth%2Freturn%2FApp7494CLIAPI%2F&response_type=code');
      });
  });

  it('can fetch an access token', () => {
    const bundle = {
      inputData: {
        secret: 'secret',
        code: 'bbr11be7Pl',
      },
      environment: {
        CLIENT_ID: getClientId(),
        CLIENT_SECRET: getSecret(),
      },
    };

    bundle.inputData.url = getURL();

    return appTester(App.authentication.oauth2Config.getAccessToken, bundle)
      .then((result) => {
        result.access_token.should.eql('a_token');
        result.refresh_token.should.eql('a_refresh_token');
      });
  });

  it('can refresh the access token', () => {
    const bundle = {
      // In production, Zapier provides these. For testing, we have hard-coded them.
      // When writing tests for your own app, you should consider exporting them and doing process.env.MY_ACCESS_TOKEN
      authData: {
        access_token: 'a_token',
        refresh_token: 'a_refresh_token'
      },
      environment: {
        CLIENT_ID: getClientId(),
        CLIENT_SECRET: getSecret(),
      },
      inputData: {},
    };

    bundle.inputData.url = getURL();

    return appTester(App.authentication.oauth2Config.refreshAccessToken, bundle)
      .then((result) => {
        result.access_token.should.eql('a_new_token');
      });
  });

  it('includes the access token in future requests', () => {
    const bundle = {
      authData: {
        access_token: 'a_token',
        refresh_token: 'a_refresh_token'
      },
      inputData: {},
    };

    bundle.inputData.url = getURL();

    return appTester(App.authentication.test, bundle)
      .then((result) => {
        result.should.have.property('username');
        result.username.should.eql('Bret');
      });
  });
});

const getClientId = () => {
  return process.env.CLIENT_ID ? process.env.CLIENT_ID : 'nuxeo-zapier';
}

const getSecret = () => {
  return process.env.CLIENT_SECRET ? process.env.CLIENT_ID : 'nuxeo-zapier';
}

const getRedirectURI = () => {
  return process.env.REDIRECT_URI ? process.env.REDIRECT_URI : 'https://zapier.com/dashboard/auth/oauth/return/App7494CLIAPI/';
}

const getURL = () => {
  return process.env.URL ? process.env.URL : 'http://zapier.apps.prod.nuxeo.io';
}
