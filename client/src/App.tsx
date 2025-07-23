import { Button } from "@/components/ui/button"
import { Routes, Route, useNavigate } from 'react-router-dom'
import BattlePage from './battle/BattlePage'
import LoginPage from './login/LoginPage'
import HomePage from './home/HomePage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/home" element={<HomePage />} />
      <Route path="/battle" element={<BattlePage />} />
    </Routes>
  )
}

export default App