import React, { useState, useEffect, useRef } from 'react';

const App = () => {
    const [routes, setRoutes] = useState({});
    const [busStops, setBusStops] = useState({});
    const [buses, setBuses] = useState({});
    const [stopInfo, setStopInfo] = useState({});
    const [busStates, setBusStates] = useState({});
    const [hoveredBus, setHoveredBus] = useState(null);

    const canvasRef = useRef(null);

    const fetchRoutes = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-routes');
            const data = await response.json();
            setRoutes(data);
        } catch (error) {
            console.error('Error fetching routes:', error);
        }
    };

    const fetchBusStops = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-bus-stops');
            const data = await response.json();
            setBusStops(data);
        } catch (error) {
            console.error('Error fetching bus stops:', error);
        }
    };

    const fetchBuses = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-coordinates');
            const data = await response.json();
            setBuses(data);
        } catch (error) {
            console.error('Error fetching bus positions:', error);
        }
    };

    const fetchStopInfo = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-stop-info');
            const data = await response.json();
            setStopInfo(data);
        } catch (error) {
            console.error('Error fetching stop info:', error);
        }
    };

    const fetchBusStates = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-bus-state');
            const data = await response.json();
            setBusStates(data);
        } catch (error) {
            console.error('Error fetching bus states:', error);
        }
    };

    useEffect(() => {
        fetchRoutes();
        fetchBusStops();
        const intervalId = setInterval(() => {
            fetchBuses();
            fetchStopInfo();
            fetchBusStates();
        }, 800);
        return () => clearInterval(intervalId);
    }, []);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const handleMouseMove = (event) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = event.clientX - rect.left;
            const mouseY = event.clientY - rect.top;
            const scaleX = canvas.width / 10000;
            const scaleY = canvas.height / 10000;

            let foundBus = null;
            Object.keys(buses).forEach((busKey) => {
                const bus = buses[busKey];
                if (bus && typeof bus.x === 'number' && typeof bus.y === 'number') {
                    const busCanvasX = bus.x * scaleX;
                    const busCanvasY = canvas.height - bus.y * scaleY;
                    const dx = mouseX - busCanvasX;
                    const dy = mouseY - busCanvasY;
                    const distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= 10) {
                        foundBus = busKey;
                    }
                }
            });
            setHoveredBus(foundBus);
        };

        const handleMouseLeave = () => {
            setHoveredBus(null);
        };

        canvas.addEventListener('mousemove', handleMouseMove);
        canvas.addEventListener('mouseleave', handleMouseLeave);

        return () => {
            canvas.removeEventListener('mousemove', handleMouseMove);
            canvas.removeEventListener('mouseleave', handleMouseLeave);
        };
    }, [buses]);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        ctx.clearRect(0, 0, width, height);

        ctx.save();
        ctx.translate(0, height);
        ctx.scale(1, -1);

        const scaleX = width / 10000;
        const scaleY = height / 10000;

        ctx.strokeStyle = 'blue';
        Object.keys(routes).forEach((routeKey) => {
            const route = routes[routeKey];
            if (Array.isArray(route) && route.length > 0) {
                ctx.beginPath();
                const firstPoint = route[0];
                ctx.moveTo(firstPoint.x * scaleX, firstPoint.y * scaleY);
                for (let i = 1; i < route.length; i++) {
                    const point = route[i];
                    ctx.lineTo(point.x * scaleX, point.y * scaleY);
                }
                ctx.stroke();
            }
        });

        ctx.fillStyle = 'green';
        Object.keys(busStops).forEach((stopKey) => {
            const stops = busStops[stopKey];
            if (Array.isArray(stops)) {
                stops.forEach((stop) => {
                    const x = stop.x * scaleX;
                    const y = stop.y * scaleY;
                    ctx.beginPath();
                    ctx.arc(x, y, 5, 0, 2 * Math.PI);
                    ctx.fill();
                });
            }
        });

        ctx.fillStyle = 'red';
        Object.keys(buses).forEach((busKey) => {
            const bus = buses[busKey];
            const state = busStates[busKey];
            if (state && state.startsWith('in_transit')) {
                if (bus && typeof bus.x === 'number' && typeof bus.y === 'number') {
                    const x = bus.x * scaleX;
                    const y = bus.y * scaleY;
                    ctx.beginPath();
                    ctx.arc(x, y, 7, 0, 2 * Math.PI);
                    ctx.fill();
                }
            }
        });

        ctx.restore();

        if (hoveredBus) {
            const bus = buses[hoveredBus];
            if (bus) {
                const busCanvasX = bus.x * scaleX;
                const busCanvasY = height - bus.y * scaleY;
                ctx.fillStyle = 'black';
                ctx.font = '16px Arial';
                ctx.fillText(hoveredBus, busCanvasX + 10, busCanvasY - 10);
            }
        }
    }, [routes, busStops, buses, busStates, hoveredBus]);

    return (
        <div style={{ textAlign: 'center', padding: '20px' }}>
            <h2>Transit Sim Dashboard</h2>
            <div
                style={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'flex-start',
                    gap: '40px',
                    marginTop: '20px'
                }}
            >
                <div>
                    <canvas
                        ref={canvasRef}
                        width={800}
                        height={600}
                        style={{ border: '1px solid black' }}
                    />
                </div>
                <div style={{ textAlign: 'left', width: '600px' }}>
                    <div style={{ marginBottom: '20px' }}>
                        <h3>Next Stop</h3>
                        <div style={{
                            height: '150px',
                            overflowY: 'auto',
                            border: '1px solid #ccc',
                            padding: '8px'
                        }}>
                            <ul>
                                {Object.keys(stopInfo).map((key) => {
                                    const { coordinate, distanceNextStop } = stopInfo[key];
                                    return (
                                        <li key={key} style={{ marginBottom: '10px' }}>
                                            <strong>{key}</strong>
                                            <br />
                                            Coordinates: (X: {coordinate.x}, Y: {coordinate.y})
                                            <br />
                                            Distance to Next Stop: {distanceNextStop}
                                        </li>
                                    );
                                })}
                            </ul>
                        </div>
                    </div>
                    <div>
                        <h3>Bus States</h3>
                        <div style={{
                            maxHeight: '200px',
                            overflowY: 'auto',
                            border: '1px solid #ccc',
                            padding: '8px'
                        }}>
                            <ul>
                                {[...Object.entries(busStates)]
                                    .reverse()
                                    .map(([busKey, state]) => (
                                        <li key={busKey} style={{ marginBottom: '10px' }}>
                                            <strong>Bus {busKey}:</strong> {state}
                                        </li>
                                    ))
                                }
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default App;
