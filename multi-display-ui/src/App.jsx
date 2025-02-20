// App.jsx
import React, { useState, useEffect, useRef } from 'react';

const App = () => {
    // State to hold routes, bus stops, and bus positions.
    const [routes, setRoutes] = useState({});
    const [busStops, setBusStops] = useState({});
    const [buses, setBuses] = useState({});
    const [stopInfo, setStopInfo] = useState({});
    // NEW: State to hold bus states (key = bus number, value = state string).
    const [busStates, setBusStates] = useState({});

    // State for the hovered bus key (bus number).
    const [hoveredBus, setHoveredBus] = useState(null);

    const canvasRef = useRef(null);

    // Fetch routes from the Controller on port 8080.
    const fetchRoutes = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-routes');
            const data = await response.json();
            setRoutes(data);
        } catch (error) {
            console.error('Error fetching routes:', error);
        }
    };

    // Fetch bus stops from the Controller on port 8080.
    const fetchBusStops = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-bus-stops');
            const data = await response.json();
            setBusStops(data);
        } catch (error) {
            console.error('Error fetching bus stops:', error);
        }
    };

    // Fetch moving bus positions from DisplayController on port 8080.
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

    // NEW: Fetch the bus states (scheduled_to_start, in_transit, destination_reached).
    const fetchBusStates = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-bus-state');
            const data = await response.json();
            setBusStates(data);
        } catch (error) {
            console.error('Error fetching bus states:', error);
        }
    };

    // When the component mounts, fetch the static routes and bus stops.
    // Then start polling for bus positions, stop info, and bus states.
    useEffect(() => {
        fetchRoutes();
        fetchBusStops();

        // Poll the bus positions, stop info, and bus states every 1000 ms
        const intervalId = setInterval(() => {
            fetchBuses();
            fetchStopInfo();
            fetchBusStates();
        }, 1000);

        // Clean up the interval on unmount
        return () => clearInterval(intervalId);
    }, []);

    // Attach mouse event listeners to the canvas for detecting hover on a bus.
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const handleMouseMove = (event) => {
            const rect = canvas.getBoundingClientRect();
            // Get mouse coordinates relative to the canvas.
            const mouseX = event.clientX - rect.left;
            const mouseY = event.clientY - rect.top;
            // Calculate scaling factors.
            const scaleX = canvas.width / 10000;
            const scaleY = canvas.height / 10000;

            let foundBus = null;
            // Loop through each bus to see if the mouse is near its drawn circle.
            Object.keys(buses).forEach((busKey) => {
                const bus = buses[busKey];
                if (bus && typeof bus.x === 'number' && typeof bus.y === 'number') {
                    // Convert bus logical coordinates to canvas (top-left) coordinates.
                    const busCanvasX = bus.x * scaleX;
                    const busCanvasY = canvas.height - bus.y * scaleY;
                    // Calculate the distance between mouse and bus center.
                    const dx = mouseX - busCanvasX;
                    const dy = mouseY - busCanvasY;
                    const distance = Math.sqrt(dx * dx + dy * dy);
                    // If within the bus's radius (7) + some tolerance (e.g., 3px), consider it hovered.
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

    // Redraw the canvas each time any of the data or hovered bus changes.
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        // Clear the canvas.
        ctx.clearRect(0, 0, width, height);

        // Save the context and apply the transformation so that (0,0) is at the bottom left.
        ctx.save();
        ctx.translate(0, height);
        ctx.scale(1, -1);

        // Determine the scaling factor (since our logical coordinates range from 0 to 10000).
        const scaleX = width / 10000;
        const scaleY = height / 10000;

        // Draw routes (as blue lines).
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

        // Draw bus stops (as green circles).
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

        // Draw moving buses (as red circles), **only if** they're in the "in_transit" state.
        ctx.fillStyle = 'red';
        Object.keys(buses).forEach((busKey) => {
            const bus = buses[busKey];
            const state = busStates[busKey]; // "scheduled_to_start...", "in_transit...", or "destination_reached..."

            // Only draw the bus if it's "in_transit".
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

        // Restore context to remove the transformation.
        ctx.restore();

        // If a bus is hovered, draw its bus number near its canvas position.
        if (hoveredBus) {
            const bus = buses[hoveredBus];
            if (bus) {
                const busCanvasX = bus.x * scaleX;
                // Because we flipped the coordinate system, we have to invert the Y again
                const busCanvasY = height - bus.y * scaleY;
                ctx.fillStyle = 'black';
                ctx.font = '16px Arial';
                ctx.fillText(hoveredBus, busCanvasX + 10, busCanvasY - 10);
            }
        }
    }, [routes, busStops, buses, busStates, hoveredBus]);

    return (
        <div style={{ textAlign: 'center', padding: '20px' }}>
            <h2>Bus Simulator</h2>

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

                {/* Info Section */}
                <div style={{ textAlign: 'left', maxWidth: '680px' }}>
                    {/* Next Stop Info */}
                    <div style={{ marginBottom: '20px' }}>
                        <h3>Next Stop</h3>
                        {/* Scrollable container for Next Stop info */}
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

                    {/* Bus States Info */}
                    <div>
                        <h3>Bus States</h3>
                        {/* Scrollable container for Bus States */}
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
                                        <li key={busKey} style={{marginBottom: '10px'}}>
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
