export interface CoordinatorOperationRequest {
    destination: string;
    file: string;
    source: string;
    mode: "FLOW" | "MANUAL";
  }