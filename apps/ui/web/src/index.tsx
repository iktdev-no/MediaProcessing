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


const wsUrl = () => {
  const protocol = "ws" // window.location.protocol;
  const host = window.location.host;
  const wsUrl = `ws://${host}/ws`;
  console.log("WsUrl", wsUrl)
  if (window.location.href.startsWith("http://localhost:3000")) {
    return "ws://localhost:8080/ws";
  } else {
    return wsUrl;
  }
}
root.render(
  <React.StrictMode>
    <Provider store={store}>
      <StompSessionProvider url={wsUrl()} connectHeaders={{}} logRawCommunication={true}
       debug={(str) => {
        if (str === "Opening Web Socket...") {
          console.log("Connecting with Web Socket...")
        }
      }}
      onUnhandledMessage={(val) => {
        console.log("Unhandled message", val)
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
