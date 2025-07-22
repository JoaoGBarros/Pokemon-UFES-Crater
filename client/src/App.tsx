import { Button } from "@/components/ui/button"
import { Routes, Route, useNavigate } from 'react-router-dom'
import BattlePage from './battle/BattlePage'
import LoginPage from './login/LoginPage'

function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-svh flex-col items-center justify-center">
      <Button onClick={() => navigate('/battle')}>Começar batalha</Button>
    </div>
  )
}

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