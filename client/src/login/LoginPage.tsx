import React, { useState, useContext, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { WebSocketContext } from "@/WebSocketContext";
import emeraldBg from "@/assets/loginBackground.png";
import { useNavigate } from "react-router-dom";

function LoginPage() {
    const navigate = useNavigate();
    const [nickname, setNickname] = useState("");
    const socket = useContext(WebSocketContext);

    const [pokemons, setPokemons] = useState<any[]>([]);
    const [selectedPokemon, setSelectedPokemon] = useState<string | null>(null);

    if (socket && socket.current) {
        socket.current.send(JSON.stringify({
            type: "retrieveAvailablePokemons"
        }));

        socket.current.onmessage = (event) => {
            const serverMessage = JSON.parse(event.data);
            if (serverMessage.type === "availablePokemon") {
                setPokemons(serverMessage.payload || []);
            }
            if (serverMessage.type === "loginSuccess") {
                navigate("/game");
            } else if (serverMessage.type === "loginError") {
                alert(serverMessage.payload);
            }
        };
    }
    
    const handleLogin = (event: React.FormEvent) => {
        event.preventDefault();

        if (socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "login",
                payload: { nickname, selectedPokemon }
            }));
        } else {
            alert("Conexão com o servidor não está pronta. Tente novamente em instantes.");
        }
    };

    useEffect(() => {
        if (!socket || !socket.current) return;
        const handler = (event: MessageEvent) => {
            const serverMessage = JSON.parse(event.data);
            if (serverMessage.type === "availablePokemon") {
                setPokemons(serverMessage.payload || []);
            }
            if (serverMessage.type === "loginSuccess") {
                navigate("/game");
            } else if (serverMessage.type === "loginError") {
                alert(serverMessage.payload);
            }
        };
        socket.current.addEventListener("message", handler);
        return () => socket.current?.removeEventListener("message", handler);
    }, [socket, navigate]);

    // Função para barra de stats
    const StatBar = ({ value, max, color }: { value: number, max: number, color: string }) => (
        <div className="w-full bg-gray-200 rounded h-2 mb-1">
            <div
                className={color + " h-2 rounded"}
                style={{ width: `${Math.min(100, (value / max) * 100)}%` }}
            ></div>
        </div>
    );

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
                            {/* Grid de pokémons */}
                            {pokemons.length > 0 && (
                                <div className="mt-4">
                                    <div className="font-semibold mb-2">Escolha seu Pokémon:</div>
                                    <div className="grid grid-cols-3 gap-4">
                                        {pokemons.map((poke, idx) => (
                                            <button
                                                key={poke.name}
                                                type="button"
                                                className={
                                                    "flex flex-col items-center border rounded-lg p-2 " +
                                                    (selectedPokemon === poke.name
                                                        ? "bg-gray-300"
                                                        : "bg-gray-50 hover:bg-gray-100")
                                                }
                                                onClick={() => setSelectedPokemon(poke.name)}
                                            >
                                                <img src={poke.sprite} alt={poke.name} className="w-16 h-16 mb-1" />
                                                <div className="font-bold text-xs mb-1">{poke.name}</div>
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                        <Button type="submit" className="w-full mt-6">
                            Login
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}

export default LoginPage;
