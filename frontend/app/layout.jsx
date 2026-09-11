import "./globals.css";

export const metadata = {
  title: "Agent Swarm | Autonomous Software Engineer",
  description: "AI-powered multi-agent software engineering system",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}