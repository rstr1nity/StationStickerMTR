package com.tr1nity.stationsticker.client.display;

// Один слайд экрана: рисует себя на холсте экрана, не зная о переходах
public interface DisplaySlide {
    void render(SlideCanvas canvas);
}
