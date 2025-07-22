import React, { useState, useContext, useEffect } from "react"
import { Button } from "@/components/ui/button"
import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { WebSocketContext } from "@/WebSocketContext"

import emeraldBg from "@/assets/loginBackground.png"
import { useNavigate } from "react-router-dom"




function LoginPage() {
    const navigate = useNavigate()
    const [nickname, setNickname] = useState("")
    const socket = useContext(WebSocketContext)




    if (socket && socket.current) {
        socket.current.onmessage = (event) => {
            const serverMessage = JSON.parse(event.data);
            switch (serverMessage.type) {
                case "loginSuccess":
                    navigate("/home");
                    break;
                case "loginError":
                    alert(serverMessage.payload.message);
                    break;
            }
        };
    }

    useEffect(() => {
        if (!socket || !socket.current) return

        const handleMessage = (event: MessageEvent) => {
            const serverMessage = JSON.parse(event.data)
            if (serverMessage.type === "loginSuccess") {
                navigate("/home")
            } else if (serverMessage.type === "loginError") {
                alert(serverMessage.payload.message)
            }
        }

        socket.current.addEventListener("message", handleMessage)
        return () => {
            socket.current?.removeEventListener("message", handleMessage)
        }
    }, [socket, navigate])

    const handleLogin = (event: React.FormEvent) => {
        event.preventDefault()

        if (socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "login",
                payload: { nickname }
            }))
        } else {
            alert("Conexão com o servidor não está pronta. Tente novamente em instantes.");
        }
    }


    return (
        <div
            className="flex min-h-svh flex-col items-center justify-center"
            style={{
                backgroundImage: `url(${emeraldBg})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                backgroundRepeat: "no-repeat"
            }}
        >
            <Card className="w-full max-w-sm bg-white">
                <CardHeader>
                    <CardTitle>Login</CardTitle>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleLogin}>
                        <div className="flex flex-col gap-6">
                            <div className="grid gap-2">
                                <Label htmlFor="nickname">Nickname</Label>
                                <Input
                                    id="nickname"
                                    type="text"
                                    placeholder="Enter your nickname"
                                    required
                                    value={nickname}
                                    onChange={e => setNickname(e.target.value)}
                                />
                            </div>
                        </div>
                        <Button type="submit" className="w-full mt-6">
                            Login
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    )
}

export default LoginPage
