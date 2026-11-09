package com.platform.api.rental.order;

import com.platform.api.common.ResultInfo;
import com.platform.api.security.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rental/orders")
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;

    public RentalOrderController(RentalOrderService rentalOrderService) {
        this.rentalOrderService = rentalOrderService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('rental:order:list')")
    public ResultInfo<Page<RentalOrderDtos.RentalOrderItem>> page(RentalOrderDtos.RentalOrderQuery query) {
        return ResultInfo.ok(rentalOrderService.page(query));
    }

    @GetMapping("/{orderId}/form")
    @PreAuthorize("@perm.has('rental:order:list')")
    public ResultInfo<RentalOrderDtos.RentalOrderForm> form(@PathVariable String orderId) {
        return ResultInfo.ok(rentalOrderService.form(orderId));
    }

    @PostMapping
    @PreAuthorize("@perm.has('rental:order:create')")
    public ResultInfo<String> create(@RequestBody RentalOrderDtos.RentalOrderForm form,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        return ResultInfo.ok(rentalOrderService.create(form, loginUser));
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("@perm.has('rental:order:update')")
    public ResultInfo<Void> update(@PathVariable String orderId,
                                   @RequestBody RentalOrderDtos.RentalOrderForm form) {
        rentalOrderService.update(orderId, form);
        return ResultInfo.ok();
    }

    @DeleteMapping("/{orderIds}")
    @PreAuthorize("@perm.has('rental:order:delete')")
    public ResultInfo<Void> delete(@PathVariable String orderIds) {
        rentalOrderService.delete(orderIds);
        return ResultInfo.ok();
    }
}
