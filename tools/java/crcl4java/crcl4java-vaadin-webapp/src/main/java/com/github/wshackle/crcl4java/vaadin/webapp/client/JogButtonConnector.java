package com.github.wshackle.crcl4java.vaadin.webapp.client;

import com.github.wshackle.crcl4java.vaadin.webapp.JogButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Window;
import com.vaadin.client.MouseEventDetailsBuilder;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.Connect;

@Connect(JogButton.class)
public class JogButtonConnector extends AbstractComponentConnector {

    private JogButtonServerRpc rpc = RpcProxy.create(JogButtonServerRpc.class, this);

    private void handleMouseTouchDown() {
        rpc.mousedown();
        getWidget().addStyleName("pressed");
        getWidget().removeStyleName("notpressed");
    }

    private void handleMouseTouchUp() {
        rpc.mouseup();
        getWidget().removeStyleName("pressed");
        getWidget().addStyleName("notpressed");
    }

    public JogButtonConnector() {
        
        
        getWidget().addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                handleMouseTouchDown();
            }
        });

        getWidget().addTouchStartHandler(new TouchStartHandler() {
            @Override
            public void onTouchStart(TouchStartEvent event) {
                handleMouseTouchDown();
            }
        });
        getWidget().addMouseUpHandler(new MouseUpHandler() {
            @Override
            public void onMouseUp(MouseUpEvent event) {
                handleMouseTouchUp();
            }
        });
        getWidget().addTouchEndHandler(new TouchEndHandler() {
            @Override
            public void onTouchEnd(TouchEndEvent event) {
                handleMouseTouchUp();
            }
        });
        getWidget().addTouchCancelHandler(new TouchCancelHandler() {
            @Override
            public void onTouchCancel(TouchCancelEvent event) {
                handleMouseTouchUp();
            }
        });
    }

    @Override
    public JogButtonWidget getWidget() {
        return (JogButtonWidget) super.getWidget();
    }

    @Override
    public JogButtonState getState() {
        return (JogButtonState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);

        // TODO: do something useful
        final String text = getState().text;
        getWidget().setText(text);
    }
}
