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
    const [invites, setInvites] = useState<{from: string, message: string}[]>([])
    const [inviteModalOpen, setInviteModalOpen] = useState(false)
    const [currentInvite, setCurrentInvite] = useState<{from: string, message: string} | null>(null)
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
                
                case "battleRequest":
                    setInvites(prev => [...prev, {
                        from: serverMessage.payload,
                        message: `Você foi desafiado para uma batalha por ${serverMessage.payload}`
                    }])
                    break;
                
                case "battleState":
                    if(serverMessage.payload.battleStatus === "BATTLE_STARTED" || serverMessage.payload.battleStatus === "BATTLE_IN_PROGRESS") {
                        navigate('/battle');
                    }

                 case "startPvpBattle":
                    const { battleId } = serverMessage.payload;
                    navigate('/battle', { state: { pvpBattleId: battleId } });
                    break;
            }
        };
    }

    useEffect(() => {
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
                type: "requestBattle",
                payload: selectedUser
            }))
            setSidePanelOpen(false)
        }
    }

    const handleOpenInviteModal = () => {
        if (invites.length > 0) {
            setCurrentInvite(invites[0])
            setInviteModalOpen(true)
        }
    }

    const handleInviteResponse = (accept: boolean) => {
        if (currentInvite && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "inviteResponse",
                payload: {
                    from: currentInvite.from,
                    accept: accept
                }
            }))
        }
        setInvites(prev => prev.slice(1))
        setInviteModalOpen(false)
        setCurrentInvite(null)
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
                <div className="relative flex flex-row items-center gap-2">
                    <Button onClick={() => setDropdownOpen(v => !v)}>
                        Usuários Online
                    </Button>
                    <button
                        className="relative ml-2 p-2 rounded-full bg-white border hover:bg-gray-100"
                        onClick={handleOpenInviteModal}
                        disabled={invites.length === 0}
                        title="Convites de batalha"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V4a2 2 0 10-4 0v1.341C7.67 7.165 6 9.388 6 12v2.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                        </svg>
                        {invites.length > 0 && (
                            <span className="absolute top-0 right-0 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white bg-red-600 rounded-full">
                                {invites.length}
                            </span>
                        )}
                    </button>
                    {dropdownOpen && (
                        <div className="absolute z-10 mt-2 w-48 bg-white border rounded shadow left-0">
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
            {inviteModalOpen && currentInvite && (
                <div className="fixed inset-0 flex items-center justify-center z-30 bg-black bg-opacity-30">
                    <div className="bg-white rounded-lg shadow-lg p-8 flex flex-col items-center">
                        <div className="mb-4 text-lg font-semibold">{currentInvite.message}</div>
                        <div className="flex gap-4">
                            <Button onClick={() => handleInviteResponse(true)}>Sim</Button>
                            <Button variant="secondary" onClick={() => handleInviteResponse(false)}>Não</Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default HomePage;