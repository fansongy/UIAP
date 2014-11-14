//
//  PGStoreKitManager.m
//  Art Of War
//
//  Created by Fansy on 2/12/14.
//
//

#import "IOSManager.h"

@implementation IOSManager

+ (IOSManager *)getInstance
{
	static IOSManager *mgr = nil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		mgr = [[self alloc] init];
		[mgr initStoreKit];
	});

	return mgr;
}

- (id)init
{
	self = [super init];
	if (self) {
		[self initData];
		return self;
	}
	return nil;
}

- (void)initData
{
	_viewController = nil;
}

- (void)showMessage:(NSString *)title Message:(NSString *)msg
{
	UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title message:msg delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil, nil];
	[alert show];
}

- (void)showLoadingView:(NSString *)title
{
	_loadingAlert= [[UIAlertView alloc] initWithTitle:title message:@"" delegate:self cancelButtonTitle:nil otherButtonTitles:nil, nil];
	[_loadingAlert show];
}

- (void)removeLoadingView
{
	[_loadingAlert dismissWithClickedButtonIndex:0 animated:YES];
}

//---------------------------------------------------------
#pragma mark - IAP
- (BOOL)canProcessPayments
{
	if ([SKPaymentQueue canMakePayments]) {
		return YES;
	} else {
		return NO;
	}
}

- (void)initStoreKit
{
	[[SKPaymentQueue defaultQueue] addTransactionObserver:self];
}


-(void)getItem:(NSString*)items
{
    if (![self canProcessPayments]) {
        NSLog(@"1.Fail-->SKPaymentQueue canMakePayments NO");
        [self removeLoadingView];
        return;
    }
    NSLog(@"1.Success-->recv item: %@", items);
//    items = @"{\"items\":[\"item0\",\"item1\",\"item2\"]}";

    NSData *data= [items dataUsingEncoding:NSUTF8StringEncoding];
    
    NSError *error = nil;
    
    id jsonObject = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
    
    if ([jsonObject isKindOfClass:[NSDictionary class]]){
        
        NSDictionary *dictionary = (NSDictionary *)jsonObject;
        
        NSLog(@"Dersialized JSON Dictionary = %@", dictionary);
        
    }else if ([jsonObject isKindOfClass:[NSArray class]]){
        
        NSArray *nsArray = (NSArray *)jsonObject;
        
        NSLog(@"Dersialized JSON Array = %@", nsArray);
        
    } else {
        
        NSLog(@"An error happened while deserializing the JSON data.");
        
    }
    
    NSDictionary *dict = (NSDictionary *)jsonObject;
    
    NSArray* arr = [dict objectForKey:@"items"];
//    NSLog(@"list is %@",arr);
    
    NSSet *set = [NSSet setWithArray:arr];
//    NSLog(@"set is %@",set);
    
    SKProductsRequest *request= [[SKProductsRequest alloc] initWithProductIdentifiers: set];
    request.delegate = self;
    [request start];
}

/**
 purchaseItem
 */
- (void)purchaseItem: (NSString *)identifier
{
	[self showLoadingView:@"Access Store..."];
	
	if (![self canProcessPayments]) {
		NSLog(@"1.Fail-->SKPaymentQueue canMakePayments NO");
		[self removeLoadingView];
		return;
	}
	NSLog(@"1.Success-->Request item List...%@", identifier);
    
	// ask for Buying
	SKProductsRequest *request= [[SKProductsRequest alloc] initWithProductIdentifiers: [NSSet setWithObject: identifier]];
	request.delegate = self;
	[request start];
}

// SKProductsRequest callback
- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response
{
	NSArray *myProduct = response.products;
	
	if (myProduct.count == 0) {
		NSLog(@"2.Fail-->can't get Item info. invalidProductIdentifiers = %@",response.invalidProductIdentifiers);
		[self removeLoadingView];
		return;
	}

	NSArray* transactions = [SKPaymentQueue defaultQueue].transactions;
	if (transactions.count > 0) {
		NSLog(@"2.Success-->find unfinished buying continue purchasing...");

		SKPaymentTransaction* transaction = [transactions firstObject];
		if (transaction.transactionState == SKPaymentTransactionStatePurchased) {
			[self completeTransaction:transaction];
			[[SKPaymentQueue defaultQueue] finishTransaction:transaction];
			return;
		}
	}

	[self removeLoadingView];
    
    if(myProduct.count > 1) //req list
    {
        NSDictionary *myDict;
        NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithCapacity: 4];
        
        for(int i  = 0;i<myProduct.count;++i)
        {
            
            //NSLog(@"----------------------");
            //NSLog(@"Product title: %@" ,[myProduct[i] localizedTitle]);
            //NSLog(@"Product description: %@" ,[myProduct[i] localizedDescription]);
            //NSLog(@"Product price: %@" ,[myProduct[i] price]);
            //NSLog(@"Product id: %@" ,[myProduct[i] productIdentifier]);
            
            myDict = [NSDictionary dictionaryWithObjectsAndKeys:
                            [myProduct[i] localizedTitle], @"title",
                            [myProduct[i] localizedDescription], @"desc",
                            [myProduct[i] price], @"price",
                            [myProduct[i] productIdentifier], @"product", nil];
            
            [dict setValue: myDict forKey: [myProduct[i] productIdentifier]];
        }
        if([NSJSONSerialization isValidJSONObject:dict])
        {
            NSError* error;
            NSData *str = [NSJSONSerialization dataWithJSONObject:dict options:kNilOptions error:&error];
            NSString *result = [[NSString alloc]initWithData:str encoding:NSUTF8StringEncoding];
            NSLog(@"Result: %@",result);
            UnitySendMessage("Bridge", "onGetItem", [result UTF8String]);
        }
        else
        {
            NSLog(@"An error happened while serializing the JSON data.");
        }
    }
    else //Buy
    {
        NSLog(@"2.Success-->get Item Purchasing...");
        SKPayment * payment = [SKPayment paymentWithProduct:myProduct[0]];
        [[SKPaymentQueue defaultQueue] addPayment:payment];
 
    }
}

// SKPayment callBack
- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions
{
	NSLog(@"3.Success-->recv message from apple...");
	for (SKPaymentTransaction *transaction in transactions){
		switch (transaction.transactionState){
			case SKPaymentTransactionStatePurchased:
				[self completeTransaction:transaction];
				break;
				
			case SKPaymentTransactionStateFailed:
				[self failedTransaction:transaction];
				break;
				
			case SKPaymentTransactionStateRestored:
				[self restoreTransaction:transaction];
				break;
				
			default:
				break;
		}
	}
}

- (NSString *)encode:(const uint8_t *)input length:(NSInteger)length {
    static char table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    
    NSMutableData *data = [NSMutableData dataWithLength:((length + 2) / 3) * 4];
    uint8_t *output = (uint8_t *)data.mutableBytes;
    
    for (NSInteger i = 0; i < length; i += 3) {
        NSInteger value = 0;
        for (NSInteger j = i; j < (i + 3); j++) {
            value <<= 8;
            
            if (j < length) {
                value |= (0xFF & input[j]);
            }
        }
        
        NSInteger index = (i / 3) * 4;
        output[index + 0] =                    table[(value >> 18) & 0x3F];
        output[index + 1] =                    table[(value >> 12) & 0x3F];
        output[index + 2] = (i + 1) < length ? table[(value >> 6)  & 0x3F] : '=';
        output[index + 3] = (i + 2) < length ? table[(value >> 0)  & 0x3F] : '=';
    }
    
    return [[[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding] autorelease];
}

// recv payment from apple
- (void) completeTransaction: (SKPaymentTransaction*)transaction
{
	NSLog(@"4.Success-->Purchase Complete SKPaymentTransactionStatePurchased");
	[self removeLoadingView];
	[self provideContent: transaction];
    
	// remove transaction from the payment queue.
	[[SKPaymentQueue defaultQueue] finishTransaction: transaction];
}

// reset purchase
- (void) restoreTransaction: (SKPaymentTransaction*)transaction
{
	NSLog(@"4.Success-->reset purchase SKPaymentTransactionStateRestored");
	[self provideContent: transaction];
	[[SKPaymentQueue defaultQueue] finishTransaction: transaction];
}

// purchase fail
- (void) failedTransaction: (SKPaymentTransaction*)transaction
{
	[self removeLoadingView];
	NSLog(@"4.Fail-->purchase fail SKPaymentTransactionStateRestored error.code:%d",(int)transaction.error.code);
	[[SKPaymentQueue defaultQueue] finishTransaction: transaction];
}

// finish purchase
- (void) provideContent: (SKPaymentTransaction*)transaction
{
	NSLog(@"4.Success-->Purchase success,provide the product");
    // use for check the result is legal
    NSString* receipt = [self encode:(uint8_t *)transaction.transactionReceipt.bytes
									   length:transaction.transactionReceipt.length];
    
	NSString* iden = transaction.transactionIdentifier;
    
    UnitySendMessage("Bridge", "onPay",iden.UTF8String);

//	std::string re = [receipt UTF8String];
//	std::string idens = [iden UTF8String];
//	std::string final = re + "-" + idens;

}

@end
