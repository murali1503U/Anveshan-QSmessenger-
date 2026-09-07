import subprocess
import sys
import os

def run():
    print("1. Creating virtual environment (.venv_graphify)...")
    subprocess.run([sys.executable, "-m", "venv", ".venv_graphify"], check=True)
    
    # Windows path for pip and graphify inside venv
    pip_exe = os.path.join(".venv_graphify", "Scripts", "pip.exe")
    graphify_exe = os.path.join(".venv_graphify", "Scripts", "graphify.exe")
    
    print("2. Installing graphifyy...")
    subprocess.run([pip_exe, "install", "graphifyy"], check=True)
    
    print("3. Indexing the codebase with Graphify...")
    subprocess.run([graphify_exe, "."], check=True)
    
    print("Done! Outputs generated in graphify-out/ directory.")

if __name__ == "__main__":
    run()
