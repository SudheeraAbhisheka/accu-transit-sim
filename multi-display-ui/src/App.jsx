// App.jsx
import React, { useState, useEffect, useRef } from 'react';

const App = () => {
    // State to hold routes, bus stops, and bus positions.
    const [routes, setRoutes] = useState({});
    const [busStops, setBusStops] = useState({});
    const [buses, setBuses] = useState({});
    const canvasRef = useRef(null);
    const [stopInfo, setStopInfo] = useState({});

    // Fetch routes from the Controller on port 8081.
    const fetchRoutes = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/get-routes');
            const data = await response.json();
            setRoutes(data);
        } catch (error) {
            console.error('Error fetching routes:', error);
        }
    };

    // Fetch bus stops from the Controller on port 8081.
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

    // When the component mounts, fetch the static routes and bus stops.
    // Then start polling for bus positions.
    useEffect(() => {
        fetchRoutes();
        fetchBusStops();

        // Poll the bus positions every 1000 ms (adjust as needed)
        const intervalId = setInterval(() => {
            fetchBuses();
            fetchStopInfo();
        }, 1000);

        // Clean up the interval on unmount.
        return () => clearInterval(intervalId);
    }, []);

    // Redraw the canvas each time any of the data changes.
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        // Clear the canvas.
        ctx.clearRect(0, 0, width, height);

        // Set the transformation so that (0,0) is at the bottom left.
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

        // Draw moving buses (as red circles).
        ctx.fillStyle = 'red';
        Object.keys(buses).forEach((busKey) => {
            const bus = buses[busKey];
            if (bus && typeof bus.x === 'number' && typeof bus.y === 'number') {
                const x = bus.x * scaleX;
                const y = bus.y * scaleY;
                ctx.beginPath();
                ctx.arc(x, y, 7, 0, 2 * Math.PI);
                ctx.fill();
            }
        });

        ctx.restore();
    }, [routes, busStops, buses]);


    return (
        <div style={{textAlign: 'center'}}>
            <h2>Bus Simulator</h2>
            <div style={{marginTop: '20px'}}>
                {/* The canvas size here can be adjusted as needed */}
                <canvas
                    ref={canvasRef}
                    width={800}
                    height={600}
                    style={{border: '1px solid black'}}
                />
            </div>
            <div>
                <h3>Stop Info</h3>
                <ul>
                    {Object.keys(stopInfo).map((key) => {
                        const {coordinate, distanceNextStop} = stopInfo[key];
                        return (
                            <li key={key}>
                                <strong>{key}</strong>
                                <br/>
                                Coordinates: (X: {coordinate.x}, Y: {coordinate.y})
                                <br/>
                                Distance to Next Stop: {distanceNextStop}
                            </li>
                        );
                    })}
                </ul>
            </div>

        </div>

    );
};

export default App;
