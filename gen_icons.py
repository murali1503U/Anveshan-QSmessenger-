import os, glob
from PIL import Image

src = r'C:/Users/ASUS/.gemini/antigravity/brain/ef44fccc-896c-4f3f-ba78-07d8021bffda/.user_uploaded/media_1788779156971.jpg'
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

img = Image.open(src)
base_dir = r'app/src/main/res/'

for folder, size in sizes.items():
    folder_path = os.path.join(base_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # Remove existing .webp or .png
    for f in glob.glob(os.path.join(folder_path, 'ic_launcher.*')):
        os.remove(f)
    for f in glob.glob(os.path.join(folder_path, 'ic_launcher_round.*')):
        os.remove(f)
        
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(os.path.join(folder_path, 'ic_launcher.png'), 'PNG')
    resized.save(os.path.join(folder_path, 'ic_launcher_round.png'), 'PNG')

# Delete anydpi xmls
anydpi_dir = os.path.join(base_dir, 'mipmap-anydpi-v26')
if os.path.exists(anydpi_dir):
    for f in glob.glob(os.path.join(anydpi_dir, '*')):
        os.remove(f)
    os.rmdir(anydpi_dir)
print('Done!')

