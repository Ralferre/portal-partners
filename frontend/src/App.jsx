import { useEffect, useState } from "react";
import api from "./services/api";

export default function App() {
  const [message, setMessage] = useState("Carregando...");

  useEffect(() => {
    api.get("/health")
       .then(res => setMessage(res.data))
       .catch(() => setMessage("Backend indisponível"));
  }, []);

  return (
    <div style={{ padding: 30, fontFamily: "Arial" }}>
      <h1>Testing App Frontend React + Vite</h1>
      <p>Status of backend application: {message}</p>
    </div>
  );
}