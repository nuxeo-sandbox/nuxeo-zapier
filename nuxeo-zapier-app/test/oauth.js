require('should');

const zapier = require('zapier-platform-core');

const App = require('../index');
const appTester = zapier.createAppTester(App);

let code = '';

describe('oauth2 app', () => {
  it('generates an authorize URL', () => {
    const bundle = {
      authData: {
        redirect_uri: getRedirectURI,
        state: 'stateless'
      },
      environment: {
        CLIENT_ID: getClientId,
        CLIENT_SECRET: getSecret(),
      },
      inputData: {
        url: getURL(),
        redirect_uri: getRedirectURI(),
      }
    };

    return appTester(App.authentication.oauth2Config.authorizeUrl, bundle)
      .then((authorizeUrl) => {
        authorizeUrl.should.eql('http://localhost:8080/nuxeo/oauth2/authorize?client_id=nuxeo-zapier' +
          '&redirect_uri=https%3A%2F%2Fzapier.com%2Fdashboard%2Fauth%2Foauth%2Freturn%2FApp7494CLIAPI%2F' +
          '&response_type=code');
      });
  });

});

const getClientId = () => {
  return process.env.CLIENT_ID ? process.env.CLIENT_ID : 'nuxeo-zapier';
};

const getSecret = () => {
  return process.env.CLIENT_SECRET ? process.env.CLIENT_ID : 'nuxeo-zapier';
};

const getRedirectURI = () => {
  return process.env.REDIRECT_URI ? process.env.REDIRECT_URI : 'https://zapier.com/dashboard/auth/oauth/return/App7494CLIAPI/';
};

const getURL = () => {
  return process.env.URL ? process.env.URL : 'http://localhost:8080';
};
