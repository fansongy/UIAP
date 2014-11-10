//
//  PGStoreKitManager.h
//  Art Of War
//
//  Created by Fansy on 2/12/14.
//
//

#import <Foundation/Foundation.h>
#import <GameKit/GameKit.h>
#import <StoreKit/StoreKit.h>

@class ViewController;
@interface IOSManager : NSObject<GKGameCenterControllerDelegate, SKPaymentTransactionObserver, SKProductsRequestDelegate>
{
	UIAlertView *_loadingAlert;
}

@property (nonatomic, readwrite, strong) ViewController* viewController;
+ (IOSManager *)getInstance;

// iap ----------------------------------------
- (void)initStoreKit;

/**
 Purchase Item
 */
- (void)purchaseItem: (NSString*)identifier;

/**
 Request Item Info
 */
-(void)getItem:(NSString*)items;
 

@end