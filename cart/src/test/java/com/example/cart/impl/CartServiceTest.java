package com.example.cart.impl;

import akka.Done;
import akka.japi.Pair;
import com.example.cart.api.Quantity;
import com.example.cart.api.CartItem;
import com.example.cart.api.CartService;
import com.example.cart.api.CartView;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;

public class CartServiceTest {

    private String randomId() {
        return UUID.randomUUID().toString();
    }

    private static ServiceTest.TestServer testServer;
    private static CartService shoppingCartService;

    @BeforeClass
    public static void startServer() {
        testServer = ServiceTest.startServer(defaultSetup().withJdbc());
        shoppingCartService = testServer.client(CartService.class);
    }

    @AfterClass
    public static void stopServer() {
        testServer.stop();
    }

    @Test
    public void shouldAddAnItem() {
        String cartId = randomId();
        String itemId = randomId();

        Pair<ResponseHeader, Done> result = Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));
        ResponseHeader responseHeader = result.first();

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());
    }

    @Test
    public void shouldRemoveAnItem() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        // And try to remove it
        Pair<ResponseHeader, CartView> result = Await.result(shoppingCartService.removeItem(cartId, itemId).withResponseHeader().invoke());
        ResponseHeader responseHeader = result.first();
        CartView cartView = result.second();

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());
        Assert.assertFalse(cartView.hasItem(itemId));
    }

    @Test
    public void shouldUpdateItemQuantity() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        // And update item quantity
        int newQuantity = 10;
        Pair<ResponseHeader, CartView> result = Await.result(shoppingCartService.adjustItemQuantity(cartId, itemId).withResponseHeader().invoke(new Quantity(newQuantity)));
        ResponseHeader responseHeader = result.first();
        CartView cartView = result.second();

        Optional<CartItem> shoppingCartItem = cartView.get(itemId);

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());

        Assert.assertTrue(shoppingCartItem.isPresent());
        shoppingCartItem.ifPresent(item -> Assert.assertEquals(item.getQuantity(), newQuantity));
    }

    @Test
    public void shouldAllowCheckingOut() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        Pair<ResponseHeader, Done> result = Await.result(shoppingCartService.checkout(cartId).withResponseHeader().invoke());
        ResponseHeader responseHeader = result.first();

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());
    }

    @Test
    public void shouldAllowGettingShoppingCartSummary() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        Pair<ResponseHeader, CartView> result = Await.result(shoppingCartService.get(cartId).withResponseHeader().invoke());
        ResponseHeader responseHeader = result.first();
        CartView cartView = result.second();

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());
        Assert.assertFalse(cartView.isCheckedOut());
        Assert.assertTrue(cartView.hasItem(itemId));
    }

    @Test
    public void shouldNotChangeTheCartWhenRemovingItemThatIsNotThere() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        String itemNotInCart = randomId();
        Pair<ResponseHeader, CartView> result = Await.result(shoppingCartService.removeItem(cartId, itemNotInCart).withResponseHeader().invoke());
        ResponseHeader responseHeader = result.first();
        CartView cartView = result.second();

        Assert.assertEquals(responseHeader.status(), ResponseHeader.OK.status());
        Assert.assertTrue(cartView.hasItem(itemId));
    }

    @Test
    public void shouldFailWhenAdjustingItemQuantityToNegativeNumber() {
        String cartId = randomId();
        String itemId = randomId();

        // Add a shopping cart item
        Await.result(shoppingCartService.addItem(cartId).withResponseHeader().invoke(new CartItem(itemId, 2)));

        // And update item quantity
        int negativeQuantity = -10;

        try {
            Await.result(
                shoppingCartService.adjustItemQuantity(cartId, itemId).invoke(new Quantity(negativeQuantity))
            );
            Assert.fail("It should fail when quantity is less than zero");
        } catch (Exception e) {
            Assert.assertTrue("Failed as expected: " + e, true);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause().getMessage().contains("Quantity must be greater than zero"));
        }
    }
}
