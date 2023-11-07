import * as Stomp from 'stompjs';
import SockJS from 'sockjs-client';

export class WebSocketClient {
    private stompClient: Stomp.Client | undefined;
    private subscriptions: { [id: string]: Stomp.Subscription } = {};
    private isConnecting = false;
    private connectCallbacks: (() => void)[] = [];
    private wsUrl: string | undefined;

  public connect(wsUrl: string) {
    this.wsUrl = wsUrl;
    if (this.isConnecting) {
      return;
    }
    this.isConnecting = true;
    const socket = new SockJS(this.wsUrl);
    this.stompClient = Stomp.over(socket);
    this.stompClient.connect({}, (frame) => {
      this.isConnecting = false;
      this.connectCallbacks.forEach((cb) => cb());
    });
  }

  public subscribe<T>(path: string, processData: (data: T) => void) {
    const subscription = this.stompClient?.subscribe(path, (data) => {
      const converted = JSON.parse(data.body) as T;
      processData(converted);
    });
    if (subscription !== undefined) {
        this.subscriptions[path] = subscription;
    }
  }

  public unsubscribe(path: string) {
    const subscription = this.subscriptions[path];
    if (subscription) {
      subscription.unsubscribe();
      delete this.subscriptions[path];
    }
  }

  public onConnect(callback: () => void) {
    if (this.isConnecting) {
      this.connectCallbacks.push(callback);
    } else {
      callback();
    }
  }

  public disconnect() {
    this.stompClient?.disconnect(() => {});
  }  
}


export const webSocketClient = new WebSocketClient();