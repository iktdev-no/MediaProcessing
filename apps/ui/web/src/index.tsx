import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { store } from './app/store';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { Provider } from 'react-redux';
import { StompSessionProvider } from 'react-stomp-hooks';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <Provider store={store}>
      <StompSessionProvider url={"http://localhost:8080/ws"} connectHeaders={{}} logRawCommunication={true}
       debug={(str) => {
        if (str === "Opening Web Socket...") {
        }
        console.log(str);
      }}
      onUnhandledMessage={(val) => {
        console.log(val)
      }}
      onStompError={(val) => {
        console.log(val)
      }}
      onChangeState={(val) => {
        console.log(val)
      }}
      
      >
        <App />
      </StompSessionProvider>
    </Provider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
