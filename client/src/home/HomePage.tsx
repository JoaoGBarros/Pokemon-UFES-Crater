import { Button } from "@/components/ui/button"
import { useNavigate } from 'react-router-dom'
import React, { useContext, useEffect, useRef, useState } from "react"
import { WebSocketContext } from "@/WebSocketContext"

function HomePage() {
    const navigate = useNavigate()
    const socket = useContext(WebSocketContext)
    const [messages, setMessages] = useState<string[]>([])
    const [input, setInput] = useState("")
    const [onlineUsers, setOnlineUsers] = useState<string[]>([])
    const [dropdownOpen, setDropdownOpen] = useState(false)
    const [selectedUser, setSelectedUser] = useState<string | null>(null)
    const [sidePanelOpen, setSidePanelOpen] = useState(false)
    const chatEndRef = useRef<HTMLDivElement>(null)

    if (socket && socket.current) {
        socket.current.onmessage = (event) => {
            const serverMessage = JSON.parse(event.data);
            switch (serverMessage.type) {
                case "currentPlayers":
                    setOnlineUsers(serverMessage.payload);
                    break;
                case "globalChat":
                    if (Array.isArray(serverMessage.payload)) {
                        setMessages(serverMessage.payload.filter(msg => typeof msg === 'string'));
                    } else if (typeof serverMessage.payload === 'string') {
                        setMessages([serverMessage.payload]);
                    }
                    break;
            }
        };
    }

    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
        if (socket && socket.current) {
            socket.current.send(JSON.stringify({
                type: "retrieveCurrentPlayers"
            }))
            socket.current.send(JSON.stringify({
                type: "retrieveGlobalChat"
            }))
        }
    }, [messages])

    const sendMessage = (e: React.FormEvent) => {
        e.preventDefault()
        if (input.trim() && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "chat",
                payload: input
            }))
            setInput("")
        }
    }

    const handleUserClick = (user: string) => {
        setSelectedUser(user)
        setSidePanelOpen(true)
        setDropdownOpen(false)
    }

    const handleChallenge = () => {
        if (selectedUser && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "challenge_request",
                to: selectedUser
            }))
            setSidePanelOpen(false)
        }
    }

    return (
        <div className="flex min-h-svh flex-col items-center justify-center relative">
            <div className="flex flex-row gap-8 w-full max-w-2xl">
                <div className="flex flex-col flex-1 border rounded-lg p-4 bg-white shadow h-[400px]">
                    <div className="font-bold mb-2">Chat</div>
                    <div className="bg-gray-100 rounded-lg p-4 h-32 overflow-y-auto">
                        <div className="text-sm space-y-1">
                            {messages.map((message, index) => (
                                <div key={index} className="text-gray-800">{message}</div>
                            ))}
                        </div>
                    </div>
                    <form onSubmit={sendMessage} className="flex gap-2">
                        <input
                            className="flex-1 border rounded px-2 py-1"
                            value={input}
                            onChange={e => setInput(e.target.value)}
                            placeholder="Digite sua mensagem..."
                        />
                        <Button type="submit">Enviar</Button>
                    </form>
                </div>
                <div className="relative">
                    <Button onClick={() => setDropdownOpen(v => !v)}>
                        Usuários Online
                    </Button>
                    {dropdownOpen && (
                        <div className="absolute z-10 mt-2 w-48 bg-white border rounded shadow">
                            {onlineUsers.length === 0 && (
                                <div className="px-4 py-2 text-gray-500">Nenhum usuário online</div>
                            )}
                            {onlineUsers.map(user => (
                                <button
                                    key={user}
                                    className="w-full text-left px-4 py-2 hover:bg-gray-100"
                                    onClick={() => handleUserClick(user)}
                                    type="button"
                                >
                                    {user}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>
            <div className="mt-8">
                <Button onClick={() => navigate('/battle')}>Começar batalha</Button>
            </div>
            {sidePanelOpen && selectedUser && (
                <div className="fixed top-0 right-0 h-full w-80 bg-white shadow-lg border-l flex flex-col p-6 z-20">
                    <div className="flex justify-between items-center mb-4">
                        <div className="font-bold text-lg">{selectedUser}</div>
                        <button onClick={() => setSidePanelOpen(false)} className="text-gray-500 hover:text-black">&times;</button>
                    </div>
                    <div className="flex-1 flex flex-col justify-center items-center">
                        <Button onClick={handleChallenge}>Desafiar para batalha</Button>
                    </div>
                </div>
            )}
        </div>
    )
}

export default HomePage;