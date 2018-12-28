# real-time-age-gender-estimation-android

Using pretrained age and gender models from [SSR-Net](https://github.com/shamangary/SSR-Net) camera images are processed in real-time with the following pipeline:

1. Camera image is fed into an on-device ML Kit firebase face detection model that outputs the locations of all faces in the image
2. The faces are then cropped from the original image and scaled to 3 channel, 64x64 bitmaps
3. These face bitmaps are fed into an age detection model and a separate gender detection model.
4. The output age and gender is overlayed atop each face in the original image.
