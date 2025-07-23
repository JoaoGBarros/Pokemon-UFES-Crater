import { Routes, Route } from 'react-router-dom';
import BattlePage from './battle/BattlePage';
import LoginPage from './login/LoginPage';
import GamePage from './game/GamePage'; // Atualizado para GamePage

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/game" element={<GamePage />} />
      <Route path="/battle" element={<BattlePage />} />
    </Routes>
  );
}

export default App;
