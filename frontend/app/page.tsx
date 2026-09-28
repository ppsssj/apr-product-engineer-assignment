export default function Home() {
  return (
    <main style={{ padding: 24 }}>
      <h1 style={{ fontSize: 20 }}>메디큐브톡</h1>
      <p style={{ color: "#666", fontSize: 14, lineHeight: 1.7 }}>
        여기서부터 만들어주세요.
        <br />
        디자인과 요구사항, API 명세는 <code>ASSIGNMENT.md</code>를 확인해주세요.
      </p>
      <p style={{ fontSize: 14 }}>
        <a href="/dev-user">개발용 유저 전환 →</a>
      </p>
    </main>
  );
}
