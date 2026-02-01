Color Hunter: RGB-Based Computer Vision Game

Color Hunter is a real-time interactive game that uses computer vision to turn your environment into a playground. 
Developed with Java and OpenCV, the game tests your speed and perception by requiring you to find and present real-world objects matching a specific RGB target color before the timer runs out.

Key Features:

RGB Color Detection: Real-time analysis of Red, Green, and Blue color channels to verify physical objects shown to the webcam.

Timed Challenge: A fast-paced gameplay loop where players must identify the correct color in their environment and gain points under time pressure.

Mirror Experience: Applied Core.flip logic to ensure the camera feed acts like a natural mirror, making the interaction intuitive for the player.

Dynamic HUD: A live Head-Up Display showing the target color, current score, and a countdown timer.

Technical Highlights:

Vision Engine: Leveraged OpenCV for Java to handle efficient RGB frame manipulation and real-time color filtering.

Concurrency & Optimization: To overcome the challenge of UI lagging during heavy image processing, I implemented a Multi-threaded architecture. By offloading frame capturing to a dedicated thread, the application maintains a stable 30 FPS experience.

HCI Design: Focused on Human-Computer Interaction principles, designing the game as a "fit-game" prototype that encourages physical movement.

Noise Reduction: Utilized morphological operations to ensure reliable color detection across various indoor lighting conditions.

The core technical challenge of this project was balancing intensive mathematical calculations on every camera frame without freezing the user interface. 
My solution involved decoupling the processing logic from the rendering engine using Java's concurrency tools. 
While currently optimized for Red, Green, and Blue, the system's modular design allows for future expansion into complex color spaces like HSV. 
This project serves as a strong demonstration of real-time system design and interactive software development.
