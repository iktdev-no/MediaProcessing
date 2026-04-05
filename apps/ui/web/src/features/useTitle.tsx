import { createContext, type ReactNode, useContext, useState } from "react";

interface TitleContextValue {
  title: string;
  setTitle: (t: string) => void;
}

const TitleContext = createContext<TitleContextValue>({
  title: "System Dashboard",
  setTitle: () => {},
});

interface TitleProviderProps {
  children: ReactNode;
}

export function TitleProvider({ children }: TitleProviderProps) {
  const [title, setTitle] = useState("System Dashboard");

  return (
    <TitleContext.Provider value={{ title, setTitle }}>
      {children}
    </TitleContext.Provider>
  );
}

export function useTitle() {
  return useContext(TitleContext);
}
